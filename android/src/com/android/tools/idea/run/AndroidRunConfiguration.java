/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;

/**
 * Run Configuration used for running Android Apps (and Instant Apps) locally on a device/emulator.
 */
public class AndroidRunConfiguration extends AndroidAppRunConfigurationBase {
  public AndroidRunConfiguration(Project project, ConfigurationFactory factory) {
    super(project, factory);
  }
}
