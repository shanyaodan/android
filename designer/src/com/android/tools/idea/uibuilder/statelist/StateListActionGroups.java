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
package com.android.tools.idea.uibuilder.statelist;

import com.android.tools.adtui.actions.SetZoomAction;
import com.android.tools.adtui.actions.ZoomInAction;
import com.android.tools.adtui.actions.ZoomLabelAction;
import com.android.tools.adtui.actions.ZoomOutAction;
import com.android.tools.adtui.actions.ZoomToFitAction;
import com.android.tools.idea.common.editor.ToolbarActionGroups;
import com.android.tools.idea.common.surface.DesignSurface;
import com.android.tools.adtui.actions.ZoomType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class StateListActionGroups extends ToolbarActionGroups {
  public StateListActionGroups(@NotNull DesignSurface surface) {
    super(surface);
  }

  @NotNull
  @Override
  protected ActionGroup getNorthGroup() {
    DefaultActionGroup group = new DefaultActionGroup();
    Arrays.asList(State.values()).forEach(state -> group.add(new ToggleStateAction(state, mySurface)));

    return group;
  }

  @NotNull
  @Override
  protected ActionGroup getNorthEastGroup() {
    DefaultActionGroup group = new DefaultActionGroup();

    group.add(ZoomOutAction.INSTANCE);
    group.add(ZoomLabelAction.INSTANCE);
    group.add(ZoomInAction.INSTANCE);
    group.add(ZoomToFitAction.INSTANCE);

    return group;
  }
}
