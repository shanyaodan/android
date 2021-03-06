/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run.editor;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.run.DeviceCount;
import com.android.tools.idea.run.LaunchCompatibilityChecker;
import com.android.tools.idea.run.TargetSelectionMode;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JList;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DeployTargetProvider<S extends DeployTargetState> {
  @VisibleForTesting
  static final ExtensionPointName<DeployTargetProvider> EP_NAME = ExtensionPointName.create("com.android.run.deployTargetProvider");

  private static List<DeployTargetProvider> ourTargets;

  public static List<DeployTargetProvider> getProviders() {
    return getProviders(StudioFlags.SELECT_DEVICE_SNAPSHOT_COMBO_BOX_VISIBLE.get());
  }

  @NotNull
  static List<DeployTargetProvider> getProviders(boolean deviceSnapshotComboBoxVisible) {
    if (ourTargets == null) {
      ourTargets = Arrays.asList(EP_NAME.getExtensions());
    }

    return filterOutDeviceAndSnapshotComboBoxProvider(ourTargets, deviceSnapshotComboBoxVisible);
  }

  @NotNull
  @VisibleForTesting
  static List<DeployTargetProvider> filterOutDeviceAndSnapshotComboBoxProvider(@NotNull List<DeployTargetProvider> providers,
                                                                               boolean selectDeviceSnapshotComboBoxVisible) {
    if (selectDeviceSnapshotComboBoxVisible) {
      return providers;
    }

    Object deviceAndSnapshotComboBox = TargetSelectionMode.DEVICE_AND_SNAPSHOT_COMBO_BOX.name();

    return providers.stream()
                    .filter(provider -> !provider.getId().equals(deviceAndSnapshotComboBox))
                    .collect(Collectors.toList());
  }

  @NotNull
  public abstract String getId();

  @NotNull
  public abstract String getDisplayName();

  @NotNull
  public abstract S createState();

  public boolean showInDevicePicker(@NotNull Executor executor) {
    return false;
  }

  protected boolean isApplicable(boolean testConfiguration, boolean deviceSnapshotComboBoxVisible) {
    return true;
  }

  public abstract DeployTargetConfigurable<S> createConfigurable(@NotNull Project project,
                                                                 @NotNull Disposable parentDisposable,
                                                                 @NotNull DeployTargetConfigurableContext ctx);

  /**
   * Returns whether the current deploy target needs to ask for user input on every launch.
   * If this method is overridden to return true, then {@link #showPrompt} must also be overridden.
   */
  public boolean requiresRuntimePrompt() {
    return false;
  }

  /**
   * Prompt the user for whatever input might be required at the time of the launch and return a customized {@link DeployTarget}.
   * A return value of null indicates that the launch should be canceled.
   *
   * @param runConfigId a unique ID identifying the run configuration context from which this is being invoked
   */
  @Nullable
  public DeployTarget<S> showPrompt(@NotNull Executor executor,
                                    @NotNull ExecutionEnvironment env,
                                    @NotNull AndroidFacet facet,
                                    @NotNull DeviceCount deviceCount,
                                    boolean androidTests,
                                    @NotNull Map<String, DeployTargetState> deployTargetStates,
                                    int runConfigId,
                                    @NotNull LaunchCompatibilityChecker compatibilityChecker) {
    throw new IllegalStateException();
  }

  public abstract DeployTarget<S> getDeployTarget();

  @NotNull
  public DeployTarget<S> getDeployTarget(@NotNull Project project) {
    return getDeployTarget();
  }
}
