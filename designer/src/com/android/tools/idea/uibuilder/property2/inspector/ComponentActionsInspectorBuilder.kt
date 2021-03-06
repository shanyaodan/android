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
package com.android.tools.idea.uibuilder.property2.inspector

import com.android.tools.adtui.actions.DropDownAction
import com.android.tools.adtui.common.secondaryPanelBackground
import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.common.surface.DesignSurface
import com.android.tools.idea.uibuilder.api.actions.DirectViewAction
import com.android.tools.idea.uibuilder.api.actions.ViewAction
import com.android.tools.idea.uibuilder.api.actions.ViewActionMenu
import com.android.tools.idea.uibuilder.handlers.ViewEditorImpl
import com.android.tools.property.panel.api.*
import com.android.tools.idea.uibuilder.model.viewHandler
import com.android.tools.idea.uibuilder.property2.NelePropertiesModel
import com.android.tools.idea.uibuilder.property2.NelePropertyItem
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import java.awt.BorderLayout
import java.lang.IllegalArgumentException
import javax.swing.JPanel

/**
 * An [InspectorBuilder] for the component actions shown on top in the Nele inspector.
 */
class ComponentActionsInspectorBuilder(private val model: NelePropertiesModel) : InspectorBuilder<NelePropertyItem> {

  override fun attachToInspector(inspector: InspectorPanel, properties: PropertiesTable<NelePropertyItem>) {
    val surface = model.surface ?: return
    val selectedComponents = surface.selectionModel.selection
    if (selectedComponents.isEmpty()) {
      return
    }
    val panel = JPanel(BorderLayout()).apply {
      background = secondaryPanelBackground
    }

    val group = DefaultActionGroup("PropertyPanelActions", false)

    val primary = selectedComponents[0]
    val parentActions = primary.parent?.viewHandler?.getPropertyActions(selectedComponents) ?: emptyList()
    if (parentActions.isNotEmpty()) {
      for (action in parentActions) {
        group.add(convertToAnAction(action, surface, selectedComponents))
      }
    }

    if (selectedComponents.size == 1) {
      val childrenActions = selectedComponents[0].viewHandler?.getPropertyActions(selectedComponents) ?: emptyList()
      if (childrenActions.isNotEmpty()) {
        if (parentActions.isNotEmpty()) {
          // Both parent and children actions are not empty
          group.addSeparator()
        }
        for (action in childrenActions) {
          group.add(convertToAnAction(action, surface, selectedComponents))
        }
      }
    }

    if (group.childrenCount != 0) {
      val actionManager = ActionManager.getInstance()
      val toolbar = actionManager.createActionToolbar(ActionPlaces.UNKNOWN, group, true)
      toolbar.setTargetComponent(panel)
      panel.add(toolbar.component.apply { background = secondaryPanelBackground }, BorderLayout.WEST)
      inspector.addComponent(panel)
    }
  }

  private fun convertToAnAction(viewAction: ViewAction, surface: DesignSurface, selectedComponent: List<NlComponent>): AnAction {
    return when (viewAction) {
      is ViewActionMenu -> NoArrowDropDownButton(viewAction, surface, selectedComponent)
      is DirectViewAction -> ViewActionWrapper(viewAction, surface, selectedComponent)
      else -> throw IllegalArgumentException("Unacceptable ViewAction class ${viewAction::javaClass}")
    }
  }
}

private class ViewActionWrapper(private val viewAction: ViewAction,
                                private val surface: DesignSurface,
                                private val nlComponents: List<NlComponent>)
  : AnAction(viewAction.label, viewAction.label, viewAction.icon) {

  override fun actionPerformed(e: AnActionEvent) {
    if (nlComponents.isEmpty()) {
      return
    }
    val primary = nlComponents[0]
    val scene = surface.scene ?: return
    val handler = primary.viewHandler ?: return
    viewAction.perform(ViewEditorImpl(primary.model, scene), handler, primary, nlComponents, e.modifiers)
  }
}

private class NoArrowDropDownButton(menu: ViewActionMenu, surface: DesignSurface, nlComponents: List<NlComponent>)
  : DropDownAction(null, menu.label, menu.icon) {

  init {
    addAll(menu.actions.map { ViewActionWrapper(it, surface, nlComponents) })
  }

  override fun hasDropDownArrow() = false
}
