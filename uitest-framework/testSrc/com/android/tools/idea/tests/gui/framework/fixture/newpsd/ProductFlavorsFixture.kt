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
package com.android.tools.idea.tests.gui.framework.fixture.newpsd

import com.android.tools.idea.tests.gui.framework.GuiTests.waitUntilShowingAndEnabled
import com.android.tools.idea.tests.gui.framework.findByType
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture
import com.android.tools.idea.tests.gui.framework.robot
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import org.fest.swing.core.GenericTypeMatcher
import org.fest.swing.edt.GuiQuery
import org.fest.swing.fixture.JListFixture
import org.fest.swing.fixture.JTreeFixture
import java.awt.Container

open class ProductFlavorsFixture constructor(
  override val ideFrameFixture: IdeFrameFixture,
  override val container: Container
) : ConfigPanelFixture() {

  fun clickAddFlavorDimension(): InputNameDialogFixture {
    clickAddButton("Add")
    val listFixture = JListFixture(robot(), getList())
    listFixture.clickItem("Add Dimension")
    return InputNameDialogFixture.find(ideFrameFixture, "Create New Flavor Dimension")
  }

  fun clickAddProductFlavor(): InputNameDialogFixture {
    clickAddButton("Add")
    val listFixture = JListFixture(robot(), getList())
    listFixture.clickItem("Add Product Flavor")
    return InputNameDialogFixture.find(ideFrameFixture, "Create New Product Flavor")
  }

  private fun getList(): JBList<*> {
    return waitUntilShowingAndEnabled<JBList<*>>(robot(), null, object : GenericTypeMatcher<JBList<*>>(JBList::class.java) {
      override fun isMatching(list: JBList<*>): Boolean {
        return list.javaClass.name == "com.intellij.ui.popup.list.ListPopupImpl\$MyList"
      }
    })
  }
}