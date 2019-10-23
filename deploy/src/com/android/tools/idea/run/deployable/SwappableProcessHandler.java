/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.run.deployable;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SwappableProcessHandler {
  Key<SwappableProcessHandler> EXTENSION_KEY = new Key<>("android.deploy.swappableprocesshandler");

  @Nullable
  Executor getExecutor();

  /**
   * Returns whether or not this ProcessHandler was created with the given {@link RunConfiguration} and {@link ExecutionTarget}.
   */
  boolean isExecutedWith(@NotNull RunConfiguration runConfiguration, @NotNull ExecutionTarget executionTarget);
}
