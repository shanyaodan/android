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

import com.android.tools.idea.run.DeviceCount;
import com.android.tools.idea.run.LaunchCompatibilityChecker;
import com.android.tools.idea.run.TargetSelectionMode;
import com.android.tools.idea.run.editor.DeployTarget;
import com.android.tools.idea.run.editor.DeployTargetConfigurable;
import com.android.tools.idea.run.editor.DeployTargetConfigurableContext;
import com.android.tools.idea.run.editor.DeployTargetProvider;
import com.android.tools.idea.run.editor.DeployTargetState;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.JComponent;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeviceAndSnapshotComboBoxTargetProvider extends DeployTargetProvider {
  @NotNull
  private final Supplier<UISettings> myGetUiSettings;

  @NotNull
  private final Predicate<String> myContainsComboBox;

  private boolean myProvidingMultipleTargets;

  @VisibleForTesting
  public DeviceAndSnapshotComboBoxTargetProvider() {
    this(UISettings::getInstance, DeviceAndSnapshotComboBoxTargetProvider::containsComboBox);
  }

  @VisibleForTesting
  DeviceAndSnapshotComboBoxTargetProvider(@NotNull Supplier<UISettings> getUiSettings, @NotNull Predicate<String> containsComboBox) {
    myGetUiSettings = getUiSettings;
    myContainsComboBox = containsComboBox;
  }

  @NotNull
  @Override
  public String getId() {
    return TargetSelectionMode.DEVICE_AND_SNAPSHOT_COMBO_BOX.name();
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Use the device/snapshot drop down";
  }

  @NotNull
  @Override
  public DeployTargetState createState() {
    return new State();
  }

  @VisibleForTesting
  static final class State extends DeployTargetState {
  }

  @Override
  protected boolean isApplicable(boolean testConfiguration, boolean deviceSnapshotComboBoxVisible) {
    return deviceSnapshotComboBoxVisible;
  }

  @NotNull
  @Override
  public DeployTargetConfigurable createConfigurable(@NotNull Project project,
                                                     @NotNull Disposable parent,
                                                     @NotNull DeployTargetConfigurableContext context) {
    return new Configurable();
  }

  private static final class Configurable implements DeployTargetConfigurable {
    @Nullable
    @Override
    public JComponent createComponent() {
      return null;
    }

    @Override
    public void resetFrom(@NotNull Object state, int id) {
    }

    @Override
    public void applyTo(@NotNull Object state, int id) {
    }
  }

  @Override
  public boolean requiresRuntimePrompt() {
    return myProvidingMultipleTargets || !isComboBoxVisible();
  }

  @VisibleForTesting
  boolean isComboBoxVisible() {
    UISettings settings = myGetUiSettings.get();

    if (settings.getPresentationMode()) {
      return false;
    }

    if (settings.getShowMainToolbar()) {
      return myContainsComboBox.test(IdeActions.GROUP_MAIN_TOOLBAR);
    }

    if (settings.getShowNavigationBar()) {
      return myContainsComboBox.test("NavBarToolBar");
    }

    return false;
  }

  private static boolean containsComboBox(@NotNull String id) {
    ActionGroup group = (ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(id);
    return ActionUtil.recursiveContainsAction(group, ActionManager.getInstance().getAction("DeviceAndSnapshotComboBox"));
  }

  void setProvidingMultipleTargets(@SuppressWarnings("SameParameterValue") boolean providingMultipleTargets) {
    myProvidingMultipleTargets = providingMultipleTargets;
  }

  @Nullable
  @Override
  public DeployTarget showPrompt(@NotNull Executor executor,
                                 @NotNull ExecutionEnvironment environment,
                                 @NotNull AndroidFacet facet,
                                 @NotNull DeviceCount count,
                                 boolean androidInstrumentedTests,
                                 @NotNull Map providerIdToStateMap,
                                 int configurationId,
                                 @NotNull LaunchCompatibilityChecker checker) {
    assert requiresRuntimePrompt();
    myProvidingMultipleTargets = false;

    SelectDeploymentTargetsDialog dialog = new SelectDeploymentTargetsDialog(facet.getModule().getProject());

    if (!dialog.showAndGet()) {
      return null;
    }

    return new DeviceAndSnapshotComboBoxTarget(dialog.getSelectedDevices());
  }

  @NotNull
  @Override
  public DeployTarget getDeployTarget() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public DeployTarget getDeployTarget(@NotNull Project project) {
    assert !myProvidingMultipleTargets;

    ActionManager manager = ActionManager.getInstance();
    DeviceAndSnapshotComboBoxAction action = (DeviceAndSnapshotComboBoxAction)manager.getAction("DeviceAndSnapshotComboBox");
    Device device = action.getSelectedDevice(project);

    return new DeviceAndSnapshotComboBoxTarget(device == null ? Collections.emptyList() : Collections.singletonList(device));
  }
}
