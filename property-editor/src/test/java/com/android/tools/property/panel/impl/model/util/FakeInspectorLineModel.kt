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
package com.android.tools.property.panel.impl.model.util

import com.android.tools.property.ptable2.PTableModel
import com.android.tools.property.panel.api.InspectorLineModel
import com.android.tools.property.panel.api.PropertyEditorModel
import com.intellij.openapi.actionSystem.AnAction

enum class FakeLineType {
  TITLE, PROPERTY, TABLE, PANEL, SEPARATOR
}

open class FakeInspectorLineModel(val type: FakeLineType) : InspectorLineModel {
  override var visible = true
  override var hidden = false
  override var focusable = true
  override var parent: InspectorLineModel? = null
  var actions = listOf<AnAction>()
  open val tableModel: PTableModel? = null
  var title: String? = null
  var editorModel: PropertyEditorModel? = null
  var expandable = false
  override var expanded = false
  val children = mutableListOf<InspectorLineModel>()
  val childProperties: List<String>
    get() = children.map { it as FakeInspectorLineModel }.map { it.editorModel!!.property.name }

  var focusWasRequested = false
    private set

  override fun requestFocus() {
    focusWasRequested = true
  }

  override fun makeExpandable(initiallyExpanded: Boolean) {
    expandable = true
    expanded = initiallyExpanded
  }
}
