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
package com.android.tools.idea.run.ui;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.update.RunningApplicationUpdater;
import com.intellij.execution.update.RunningApplicationUpdaterProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import icons.StudioIcons;
import javax.swing.Icon;

import org.jetbrains.android.util.AndroidCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyChangesAction extends BaseAction {

  public static final String ID = "android.deploy.ApplyChanges";

  public static final Key<Boolean> KEY = Key.create(ID);

  public static final String NAME = "Apply Changes and Restart Activity";

  private static final String DESC = "Attempt to apply resource and code changes and restart activity.";

  public ApplyChangesAction() {
    super(NAME, KEY, StudioIcons.Shell.Toolbar.APPLY_ALL_CHANGES, DESC);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    DisableMessage message = disableForTestProject(e.getProject());
    if (message != null) {
      disableAction(e.getPresentation(), message);
    }
  }

  private static DisableMessage disableForTestProject(Project project) {
    if (project == null) {
      return null;
    }
    // Disable "Apply Changes" for any kind of test project.
    RunnerAndConfigurationSettings runConfig = RunManager.getInstance(project).getSelectedConfiguration();
    if (runConfig != null) {
      ConfigurationType type = runConfig.getType();
      String id = type.getId();
      if (AndroidCommonUtils.isTestConfiguration(id) || AndroidCommonUtils.isInstrumentationTestConfiguration(id)) {
        return new DisableMessage(DisableMessage.DisableMode.DISABLED, "test project",
                                                              "the selected configuration is a test configuration");
      }
    }
    return null;
  }

  public static class UpdaterProvider implements RunningApplicationUpdaterProvider {

    @Nullable
    @Override
    public RunningApplicationUpdater createUpdater(@NotNull Project project,
                                                   @NotNull ProcessHandler process) {

      if (getDisableMessage(project) != null || disableForTestProject(project) != null) {
        return null;
      }
      return new RunningApplicationUpdater() {
        @Override
        public String getDescription() {
          return NAME;
        }

        @Override
        public String getShortName() {
          return NAME;
        }

        @Override
        public Icon getIcon() {
          return StudioIcons.Shell.Toolbar.APPLY_ALL_CHANGES;
        }

        @Override
        public void performUpdate(AnActionEvent event) {
          new ApplyChangesAction().actionPerformed(event);
        }
      };
    }
  }
}

