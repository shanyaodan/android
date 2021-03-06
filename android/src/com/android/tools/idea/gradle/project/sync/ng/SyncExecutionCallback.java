/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.ng;

import com.android.tools.idea.gradle.project.sync.ng.variantonly.VariantOnlyProjectModels;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.util.ActionCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SyncExecutionCallback extends ActionCallback {
  @Nullable private SyncProjectModels mySyncModels;
  @Nullable private VariantOnlyProjectModels myVariantOnlyModels;
  @Nullable private Throwable mySyncError;
  @Nullable private ExternalSystemTaskId myTaskId;

  static class Factory {
    @NotNull
    SyncExecutionCallback create() {
      return new SyncExecutionCallback();
    }
  }

  @Nullable
  public SyncProjectModels getSyncModels() {
    return mySyncModels;
  }

  @Nullable
  public VariantOnlyProjectModels getVariantOnlyModels() {
    return myVariantOnlyModels;
  }

  @Nullable
  public ExternalSystemTaskId getTaskId() {
    return myTaskId;
  }

  void setDone(@Nullable SyncProjectModels models, @NotNull ExternalSystemTaskId taskId) {
    mySyncModels = models;
    myTaskId = taskId;
    setDone();
  }

  void setDone(@Nullable VariantOnlyProjectModels models, @NotNull ExternalSystemTaskId taskId) {
    myVariantOnlyModels = models;
    myTaskId = taskId;
    setDone();
  }

  @Nullable
  Throwable getSyncError() {
    return mySyncError;
  }

  void setRejected(@NotNull Throwable error) {
    mySyncError = error;
    setRejected();
  }
}
