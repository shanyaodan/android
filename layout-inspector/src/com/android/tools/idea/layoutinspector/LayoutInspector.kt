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
package com.android.tools.idea.layoutinspector

import com.android.tools.idea.layoutinspector.model.InspectorModel
import com.android.tools.idea.layoutinspector.transport.InspectorClient
import kotlin.properties.Delegates

class LayoutInspector(layoutInspectorModel: InspectorModel) {
  val modelChangeListeners = mutableListOf<(InspectorModel, InspectorModel) -> Unit>()
  val client = InspectorClient.instance

  var layoutInspectorModel: InspectorModel by Delegates.observable(layoutInspectorModel) { _, old, new ->
    modelChangeListeners.forEach { it(old, new) }
  }
}
