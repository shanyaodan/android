/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.run.deployment;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.run.AndroidRunConfiguration;
import com.android.tools.idea.run.AndroidRunConfigurationBase;
import com.android.tools.idea.run.TargetSelectionMode;
import com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.*;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.ui.popup.PopupFactoryImpl.ActionGroupPopup;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.ui.JBUI;
import icons.StudioIcons;
import org.jetbrains.android.actions.RunAndroidAvdManagerAction;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.GroupLayout.Group;
import java.awt.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DeviceAndSnapshotComboBoxAction extends ComboBoxAction {
  @VisibleForTesting
  static final String SELECTED_DEVICE = "DeviceAndSnapshotComboBoxAction.selectedDevice";

  private static final String SELECTION_TIME = "DeviceAndSnapshotComboBoxAction.selectionTime";

  /**
   * Run configurations that aren't {@link AndroidRunConfiguration} or {@link AndroidTestRunConfiguration} can use this key
   * to express their applicability for DeviceAndSnapshotComboBoxAction by setting it to true in their user data.
   */
  public static final Key<Boolean> DEPLOYS_TO_LOCAL_DEVICE = Key.create("DeviceAndSnapshotComboBoxAction.deploysToLocalDevice");

  private final Supplier<Boolean> mySelectDeviceSnapshotComboBoxVisible;
  private final Supplier<Boolean> mySelectDeviceSnapshotComboBoxSnapshotsEnabled;

  private final Function<Project, AsyncDevicesGetter> myDevicesGetterGetter;

  @NotNull
  private final Function<Project, PropertiesComponent> myGetProperties;

  private final AnAction myRunOnMultipleDevicesAction;
  private final AnAction myOpenAvdManagerAction;
  private final Clock myClock;

  private String mySelectedSnapshot;

  @SuppressWarnings("unused")
  private DeviceAndSnapshotComboBoxAction() {
    this(() -> StudioFlags.SELECT_DEVICE_SNAPSHOT_COMBO_BOX_VISIBLE.get(),
         () -> StudioFlags.SELECT_DEVICE_SNAPSHOT_COMBO_BOX_SNAPSHOTS_ENABLED.get(),
         project -> ServiceManager.getService(project, AsyncDevicesGetter.class),
         PropertiesComponent::getInstance,
         Clock.systemDefaultZone());
  }

  @VisibleForTesting
  public DeviceAndSnapshotComboBoxAction(@NotNull Supplier<Boolean> selectDeviceSnapshotComboBoxVisible,
                                         @NotNull Supplier<Boolean> selectDeviceSnapshotComboBoxSnapshotsEnabled,
                                         @NotNull Function<Project, AsyncDevicesGetter> devicesGetterGetter,
                                         @NotNull Function<Project, PropertiesComponent> getProperties,
                                         @NotNull Clock clock) {
    mySelectDeviceSnapshotComboBoxVisible = selectDeviceSnapshotComboBoxVisible;
    mySelectDeviceSnapshotComboBoxSnapshotsEnabled = selectDeviceSnapshotComboBoxSnapshotsEnabled;

    myDevicesGetterGetter = devicesGetterGetter;
    myGetProperties = getProperties;

    myRunOnMultipleDevicesAction = new RunOnMultipleDevicesAction();
    myOpenAvdManagerAction = new RunAndroidAvdManagerAction();

    Presentation presentation = myOpenAvdManagerAction.getTemplatePresentation();

    presentation.setIcon(StudioIcons.Shell.Toolbar.DEVICE_MANAGER);
    presentation.setText("Open AVD Manager");

    myClock = clock;
  }

  boolean areSnapshotsEnabled() {
    return mySelectDeviceSnapshotComboBoxSnapshotsEnabled.get();
  }

  @NotNull
  @VisibleForTesting
  final AnAction getRunOnMultipleDevicesAction() {
    return myRunOnMultipleDevicesAction;
  }

  @NotNull
  @VisibleForTesting
  final AnAction getOpenAvdManagerAction() {
    return myOpenAvdManagerAction;
  }

  @NotNull
  public List<Device> getDevices(@NotNull Project project) {
    List<Device> devices = myDevicesGetterGetter.apply(project).get();
    devices.sort(new DeviceComparator());

    return devices;
  }

  @Nullable
  final Device getSelectedDevice(@NotNull Project project) {
    List<Device> devices = getDevices(project);

    if (devices.isEmpty()) {
      return null;
    }

    PropertiesComponent properties = myGetProperties.apply(project);
    Object key = properties.getValue(SELECTED_DEVICE);

    Optional<Device> optionalSelectedDevice = devices.stream()
      .filter(device -> device.getKey().equals(key))
      .findFirst();

    if (!optionalSelectedDevice.isPresent()) {
      return devices.get(0);
    }

    Device selectedDevice = optionalSelectedDevice.get();

    if (selectedDevice.isConnected()) {
      return selectedDevice;
    }

    Optional<Device> optionalConnectedDevice = devices.stream()
      .filter(Device::isConnected)
      .findFirst();

    if (optionalConnectedDevice.isPresent()) {
      Device connectedDevice = optionalConnectedDevice.get();

      Instant connectionTime = connectedDevice.getConnectionTime();
      assert connectionTime != null : "connected device \"" + connectedDevice + "\" has a null connection time";

      if (getSelectionTime(selectedDevice, properties).isBefore(connectionTime)) {
        return connectedDevice;
      }
    }

    return selectedDevice;
  }

  @NotNull
  private static Instant getSelectionTime(@NotNull Device device, @NotNull PropertiesComponent properties) {
    CharSequence time = properties.getValue(SELECTION_TIME);

    if (time == null) {
      // I don't know why this happens
      Logger.getInstance(DeviceAndSnapshotComboBoxAction.class).warn("selected device \"" + device + "\" has a null selection time string");

      return Instant.MIN;
    }

    return Instant.parse(time);
  }

  final void setSelectedDevice(@NotNull Project project, @Nullable Device selectedDevice) {
    PropertiesComponent properties = myGetProperties.apply(project);

    if (selectedDevice == null) {
      properties.unsetValue(SELECTED_DEVICE);
      properties.unsetValue(SELECTION_TIME);
    }
    else {
      properties.setValue(SELECTED_DEVICE, selectedDevice.getKey());
      properties.setValue(SELECTION_TIME, myClock.instant().toString());
    }

    updateExecutionTargetManager(project, selectedDevice);
  }

  @Nullable
  final String getSelectedSnapshot() {
    return mySelectedSnapshot;
  }

  final void setSelectedSnapshot(@Nullable String selectedSnapshot) {
    mySelectedSnapshot = selectedSnapshot;
  }

  @NotNull
  @Override
  public final JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
    JComponent panel = new JPanel(null);
    GroupLayout layout = new GroupLayout(panel);
    Component button = createComboBoxButton(presentation);

    Group horizontalGroup = layout.createSequentialGroup()
      .addComponent(button, 0, JBUI.scale(GroupLayout.DEFAULT_SIZE), JBUI.scale(Short.MAX_VALUE))
      .addGap(JBUI.scale(3));

    Group verticalGroup = layout.createParallelGroup()
      .addComponent(button);

    layout.setHorizontalGroup(horizontalGroup);
    layout.setVerticalGroup(verticalGroup);

    panel.setLayout(layout);
    return panel;
  }

  @NotNull
  @Override
  protected final ComboBoxButton createComboBoxButton(@NotNull Presentation presentation) {
    ComboBoxButton button = new ComboBoxButton(presentation) {
      @Override
      protected JBPopup createPopup(@NotNull Runnable runnable) {
        DataContext context = getDataContext();

        ActionGroup group = createPopupActionGroup(this, context);
        boolean show = shouldShowDisabledActions();
        int count = getMaxRows();
        Condition<AnAction> condition = getPreselectCondition();

        ListPopupImpl popup = new ActionGroupPopup(null, group, context, false, true, show, false, runnable, count, condition, null, true);
        popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));

        // noinspection unchecked
        popup.getList().setCellRenderer(new CellRenderer(popup));

        return popup;
      }
    };

    button.setName("deviceAndSnapshotComboBoxButton");
    return button;
  }

  @Override
  protected final boolean shouldShowDisabledActions() {
    return true;
  }

  @NotNull
  @Override
  protected final DefaultActionGroup createPopupActionGroup(@NotNull JComponent button) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  protected final DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext context) {
    DefaultActionGroup group = new DefaultActionGroup();

    Project project = context.getData(CommonDataKeys.PROJECT);
    assert project != null;

    Collection<AnAction> actions = newSelectDeviceAndSnapshotActions(project);
    group.addAll(actions);

    if (!actions.isEmpty()) {
      group.addSeparator();
    }

    group.add(myRunOnMultipleDevicesAction);
    group.add(myOpenAvdManagerAction);

    AnAction action = getTroubleshootDeviceConnectionsAction();

    if (action == null) {
      return group;
    }

    group.addSeparator();
    group.add(action);

    return group;
  }

  @NotNull
  private Collection<AnAction> newSelectDeviceAndSnapshotActions(@NotNull Project project) {
    Map<Boolean, List<Device>> connectednessToDeviceMap = getDevices(project).stream().collect(Collectors.groupingBy(Device::isConnected));

    Collection<Device> connectedDevices = connectednessToDeviceMap.getOrDefault(true, Collections.emptyList());
    Collection<Device> disconnectedDevices = connectednessToDeviceMap.getOrDefault(false, Collections.emptyList());

    boolean connectedDevicesPresent = !connectedDevices.isEmpty();
    Collection<AnAction> actions = new ArrayList<>(connectedDevices.size() + disconnectedDevices.size() + 3);

    if (connectedDevicesPresent) {
      actions.add(new Heading("Running devices"));
    }

    connectedDevices.stream()
      .map(device -> newSelectDeviceAndSnapshotAction(project, device))
      .forEach(actions::add);

    boolean disconnectedDevicesPresent = !disconnectedDevices.isEmpty();

    if (connectedDevicesPresent && disconnectedDevicesPresent) {
      actions.add(Separator.create());
    }

    if (disconnectedDevicesPresent) {
      actions.add(new Heading("Available devices"));
    }

    disconnectedDevices.stream()
      .map(device -> newSelectDeviceAndSnapshotAction(project, device))
      .forEach(actions::add);

    return actions;
  }

  @NotNull
  private AnAction newSelectDeviceAndSnapshotAction(@NotNull Project project, @NotNull Device device) {
    return new SelectDeviceAndSnapshotAction.Builder()
      .setComboBoxAction(this)
      .setProject(project)
      .setDevice(device)
      .build();
  }

  @Nullable
  private static AnAction getTroubleshootDeviceConnectionsAction() {
    AnAction action = ActionManager.getInstance().getAction("DeveloperServices.ConnectionAssistant");

    if (action == null) {
      return null;
    }

    Presentation presentation = action.getTemplatePresentation();

    presentation.setIcon(null);
    presentation.setText("Troubleshoot device connections");

    return action;
  }

  @Override
  public final void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();

    if (!mySelectDeviceSnapshotComboBoxVisible.get()) {
      presentation.setVisible(false);
      return;
    }

    Project project = event.getProject();
    if (project == null) {
      return;
    }
    if (!AndroidUtils.hasAndroidFacets(project)) {
      presentation.setVisible(false);
      return;
    }

    presentation.setVisible(true);
    updatePresentation(presentation, RunManager.getInstance(project).getSelectedConfiguration());

    List<Device> devices = getDevices(project);
    devices.sort(new DeviceComparator());

    if (devices.isEmpty()) {
      presentation.setIcon(null);
      presentation.setText("No devices");

      return;
    }

    updateSelectedSnapshot(project);

    Device device = getSelectedDevice(project);
    assert device != null;

    presentation.setIcon(device.getIcon());

    String name = Devices.getName(device, devices);
    presentation.setText(mySelectedSnapshot == null ? name : name + " - " + mySelectedSnapshot, false);

    updateExecutionTargetManager(project, device);
  }

  @VisibleForTesting
  static void updatePresentation(@NotNull Presentation presentation, @Nullable RunnerAndConfigurationSettings settings) {
    if (settings == null) {
      presentation.setDescription("Add a run/debug configuration");
      presentation.setEnabled(false);

      return;
    }

    RunProfile configuration = settings.getConfiguration();

    // Run configurations can explicitly specify they target a local device through DEPLOY_TO_LOCAL_DEVICE.
    if (configuration instanceof UserDataHolder) {
      Boolean deploysToLocalDevice = ((UserDataHolder)configuration).getUserData(DEPLOYS_TO_LOCAL_DEVICE);
      if (deploysToLocalDevice != null && deploysToLocalDevice.booleanValue()) {
        presentation.setDescription(null);
        presentation.setEnabled(true);

        return;
      }
    }

    if (!(configuration instanceof AndroidRunConfiguration || configuration instanceof AndroidTestRunConfiguration)) {
      presentation.setDescription("Not applicable for the \"" + configuration.getName() + "\" configuration");
      presentation.setEnabled(false);

      return;
    }

    if (configuration instanceof AndroidTestRunConfiguration &&
        isFirebaseTestLabDeviceMatrixTargetSelected((AndroidRunConfigurationBase)configuration)) {
      presentation.setDescription("Not applicable for the Firebase test lab device matrix target");
      presentation.setEnabled(false);

      return;
    }

    presentation.setDescription(null);
    presentation.setEnabled(true);
  }

  private static boolean isFirebaseTestLabDeviceMatrixTargetSelected(@NotNull AndroidRunConfigurationBase configuration) {
    Object id = TargetSelectionMode.FIREBASE_DEVICE_MATRIX.name();
    return configuration.getDeployTargetContext().getCurrentDeployTargetProvider().getId().equals(id);
  }

  private void updateSelectedSnapshot(@NotNull Project project) {
    if (!mySelectDeviceSnapshotComboBoxSnapshotsEnabled.get()) {
      return;
    }

    Device device = getSelectedDevice(project);
    assert device != null;

    Collection<String> snapshots = device.getSnapshots();

    if (mySelectedSnapshot == null) {
      Optional<String> selectedDeviceSnapshot = snapshots.stream().findFirst();
      selectedDeviceSnapshot.ifPresent(snapshot -> setSelectedSnapshot(snapshot));

      return;
    }

    if (snapshots.contains(mySelectedSnapshot)) {
      return;
    }

    Optional<String> selectedSnapshot = snapshots.stream().findFirst();
    setSelectedSnapshot(selectedSnapshot.orElse(null));
  }

  private static void updateExecutionTargetManager(@NotNull Project project, @Nullable Device device) {
    ExecutionTarget target = ExecutionTargetManager.getInstance(project).getActiveTarget();

    // Skip updating ExecutionTargetManager if the Device has not meaningfully changed.
    if (device == null && target == DefaultExecutionTarget.INSTANCE ||
        target instanceof DeviceAndSnapshotExecutionTargetProvider.Target &&
        Objects.equals(device, ((DeviceAndSnapshotExecutionTargetProvider.Target)target).getDevice())) {
      return;
    }

    // In certain test scenarios, this action may get updated in the main test thread instead of the EDT thread (is this correct?).
    // So we'll just make sure the following gets run on the EDT thread and wait for its result.
    ApplicationManager.getApplication().invokeAndWait(() -> {
      RunManager runManager = RunManager.getInstance(project);
      RunnerAndConfigurationSettings settings = runManager.getSelectedConfiguration();

      // There is a bug in {@link com.intellij.execution.impl.RunManagerImplKt#clear(boolean)} where it's possible the selected setting's
      // RunConfiguration is be non-existent in the RunManager. This happens when temporary/shared RunnerAndConfigurationSettings are
      // cleared from the list of RunnerAndConfigurationSettings, and the selected RunnerAndConfigurationSettings is temporary/shared and
      // left dangling.
      if (settings == null || runManager.findSettings(settings.getConfiguration()) == null) {
        return;
      }

      ExecutionTargetManager manager = ExecutionTargetManager.getInstance(project);

      for (ExecutionTarget availableTarget : manager.getTargetsFor(settings.getConfiguration())) {
        if (availableTarget instanceof DeviceAndSnapshotExecutionTargetProvider.Target) {
          manager.setActiveTarget(availableTarget);
          break;
        }
      }
    });
  }
}
