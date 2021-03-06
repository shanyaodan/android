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
package com.android.tools.idea.uibuilder.property2

import com.android.SdkConstants.ABSOLUTE_LAYOUT
import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_BACKGROUND
import com.android.SdkConstants.ATTR_CONTENT_DESCRIPTION
import com.android.SdkConstants.ATTR_FONT_FAMILY
import com.android.SdkConstants.ATTR_LAYOUT_HEIGHT
import com.android.SdkConstants.ATTR_LAYOUT_TO_END_OF
import com.android.SdkConstants.ATTR_LAYOUT_WIDTH
import com.android.SdkConstants.ATTR_LINE_SPACING_EXTRA
import com.android.SdkConstants.ATTR_PARENT_TAG
import com.android.SdkConstants.ATTR_SRC
import com.android.SdkConstants.ATTR_SRC_COMPAT
import com.android.SdkConstants.ATTR_TEXT
import com.android.SdkConstants.ATTR_TEXT_APPEARANCE
import com.android.SdkConstants.ATTR_TEXT_COLOR
import com.android.SdkConstants.ATTR_TEXT_SIZE
import com.android.SdkConstants.ATTR_VISIBILITY
import com.android.SdkConstants.BUTTON
import com.android.SdkConstants.FRAME_LAYOUT
import com.android.SdkConstants.IMAGE_VIEW
import com.android.SdkConstants.LINEAR_LAYOUT
import com.android.SdkConstants.NEW_ID_PREFIX
import com.android.SdkConstants.RELATIVE_LAYOUT
import com.android.SdkConstants.TEXT_VIEW
import com.android.SdkConstants.TOOLS_URI
import com.android.SdkConstants.VIEW_MERGE
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.tools.adtui.model.stdui.EDITOR_NO_ERROR
import com.android.tools.adtui.model.stdui.EditingErrorCategory
import com.android.tools.idea.common.fixtures.ComponentDescriptor
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.uibuilder.property2.NelePropertiesModelTest.Companion.waitUntilEventsProcessed
import com.android.tools.idea.uibuilder.property2.support.ToggleShowResolvedValueAction
import com.android.tools.idea.uibuilder.property2.testutils.MinApiLayoutTestCase
import com.android.tools.idea.uibuilder.property2.testutils.SupportTestUtil
import com.android.tools.idea.uibuilder.scene.SyncLayoutlibSceneManager
import com.android.tools.property.panel.api.PropertiesModel
import com.android.tools.property.panel.api.PropertiesModelListener
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.TwoColorsIcon
import icons.StudioIcons
import org.intellij.lang.annotations.Language
import org.jetbrains.android.AndroidTestBase
import org.jetbrains.android.ComponentStack
import org.jetbrains.android.dom.AndroidDomUtil
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.awt.Color

private const val STRINGS = """<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string name="demo">Demo String</string>
  <string name="design">Design Demo</string>
  <dimen name="lineSpacing">13sp</dimen>
</resources>
"""

private const val HELLO_WORLD = "Hello World"

private val ERROR = EditingErrorCategory.ERROR

class NelePropertyItemTest {

  @JvmField
  @Rule
  val projectRule = AndroidProjectRule.withSdk()

  @JvmField
  @Rule
  val edtRule = EdtRule()

  private var componentStack: ComponentStack? = null

  @Before
  fun setUp() {
    MinApiLayoutTestCase.setUpManifest(projectRule.fixture)
    projectRule.fixture.testDataPath = AndroidTestBase.getModulePath("designer") + "/testData"
    projectRule.fixture.addFileToProject("/res/values/strings.xml", STRINGS)
    componentStack = ComponentStack(projectRule.project)
  }

  @After
  fun tearDown() {
    componentStack!!.restoreComponents()
    componentStack = null
  }

  @RunsInEdt
  @Test
  fun testTextProperty() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    property.model.showResolvedValues = false
    assertThat(property.name).isEqualTo(ATTR_TEXT)
    assertThat(property.namespace).isEqualTo(ANDROID_URI)
    assertThat(property.type).isEqualTo(NelePropertyType.STRING)
    assertThat(property.value).isEqualTo("@string/demo")
    assertThat(property.isReference).isTrue()
    assertThat(property.resolvedValue).isEqualTo("Demo String")
    assertThat(property.tooltipForName).isEqualTo(EXPECTED_TEXT_TOOLTIP)
    assertThat(property.editingSupport.validation("Some")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(property.libraryName).isEmpty()
    assertThat(property.components).hasSize(1)
    assertThat(property.components[0].tagName).isEqualTo(TEXT_VIEW)
    assertThat(property.colorButton).isNull()
    val browseButton = property.browseButton!!
    assertThat(browseButton.actionIcon).isEqualTo(StudioIcons.Common.PROPERTY_BOUND)
  }

  @RunsInEdt
  @Test
  fun testUnboundTextProperty() {
    val util = SupportTestUtil(projectRule, createTextViewWithHardcodedValue())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    assertThat(property.name).isEqualTo(ATTR_TEXT)
    assertThat(property.namespace).isEqualTo(ANDROID_URI)
    assertThat(property.type).isEqualTo(NelePropertyType.STRING)
    assertThat(property.value).isEqualTo("Hardcoded string")
    assertThat(property.isReference).isFalse()
    assertThat(property.resolvedValue).isEqualTo("Hardcoded string")
    assertThat(property.colorButton).isNull()
    val browseButton = property.browseButton!!
    assertThat(browseButton.actionIcon).isEqualTo(StudioIcons.Common.PROPERTY_UNBOUND)
  }

  @RunsInEdt
  @Test
  fun testTextDesignProperty() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    val design = property.designProperty
    property.model.showResolvedValues = false
    assertThat(design.name).isEqualTo(ATTR_TEXT)
    assertThat(design.namespace).isEqualTo(TOOLS_URI)
    assertThat(design.type).isEqualTo(NelePropertyType.STRING)
    assertThat(design.value).isEqualTo("@string/design")
    assertThat(design.rawValue).isEqualTo("@string/design")
    assertThat(design.isReference).isTrue()
    assertThat(design.resolvedValue).isEqualTo("Design Demo")
    assertThat(design.tooltipForName).isEqualTo("<html><b>tools:text:</b><br/>Text to display.</html>")
    assertThat(property.editingSupport.validation("Some")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(design.libraryName).isEmpty()
    assertThat(design.components).hasSize(1)
    assertThat(design.components[0].tagName).isEqualTo(TEXT_VIEW)
    assertThat(property.colorButton).isNull()
    val browseButton = property.browseButton!!
    assertThat(browseButton.actionIcon).isEqualTo(StudioIcons.Common.PROPERTY_BOUND)
    assertThat(design.designProperty).isEqualTo(design)
  }

  @RunsInEdt
  @Test
  fun testColorPropertyWithColorWithoutValue() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT_COLOR, NelePropertyType.COLOR_STATE_LIST)
    assertThat(property.name).isEqualTo(ATTR_TEXT_COLOR)
    assertThat(property.namespace).isEqualTo(ANDROID_URI)
    assertThat(property.type).isEqualTo(NelePropertyType.COLOR_STATE_LIST)
    assertThat(property.value).isNull()
    assertThat(property.isReference).isFalse()
    val colorButton = property.colorButton!!
    assertThat(colorButton.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Extras.PIPETTE)
    val browseButton = property.browseButton!!
    assertThat(browseButton.actionIcon).isEqualTo(StudioIcons.Common.PROPERTY_UNBOUND)
  }

  @RunsInEdt
  @Test
  fun testColorPropertyWithColorConstant() {
    val util = SupportTestUtil(projectRule, createTextViewWithTextColor("#FF990033"))
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT_COLOR, NelePropertyType.COLOR_STATE_LIST)
    assertThat(property.name).isEqualTo(ATTR_TEXT_COLOR)
    assertThat(property.namespace).isEqualTo(ANDROID_URI)
    assertThat(property.type).isEqualTo(NelePropertyType.COLOR_STATE_LIST)
    assertThat(property.value).isEqualTo("#FF990033")
    assertThat(property.isReference).isFalse()
    val colorIcon = ColorIcon(16, Color(0x990033))
    val colorButton = property.colorButton!!
    assertThat(colorButton.actionIcon).isEqualTo(colorIcon)
    val browseButton = property.browseButton!!
    assertThat(browseButton.actionIcon).isEqualTo(StudioIcons.Common.PROPERTY_UNBOUND)
  }

  @RunsInEdt
  @Test
  fun testColorPropertyWithColorStateList() {
    val util = SupportTestUtil(projectRule, createTextViewWithTextColor("@android:color/primary_text_dark"))
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT_COLOR, NelePropertyType.COLOR_STATE_LIST)
    property.model.showResolvedValues = false
    assertThat(property.name).isEqualTo(ATTR_TEXT_COLOR)
    assertThat(property.namespace).isEqualTo(ANDROID_URI)
    assertThat(property.type).isEqualTo(NelePropertyType.COLOR_STATE_LIST)
    assertThat(property.value).isEqualTo("@android:color/primary_text_dark")
    assertThat(property.isReference).isTrue()
    val colorIcon = TwoColorsIcon(16, Color(0xFFFFFF), Color(0x000000))
    val colorButton = property.colorButton!!
    assertThat(colorButton.actionIcon).isEqualTo(colorIcon)
    val browseButton = property.browseButton!!
    assertThat(browseButton.actionIcon).isEqualTo(StudioIcons.Common.PROPERTY_BOUND)
  }

  @RunsInEdt
  @Test
  fun testIsReference() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    assertThat(isReferenceValue(property, "hello")).isFalse()
    assertThat(isReferenceValue(property, "@string/hello")).isTrue()
    assertThat(isReferenceValue(property, "@android:string/hello")).isTrue()
    assertThat(isReferenceValue(property, "?backgroundColor")).isTrue()
    // IDs should not be treated as references:
    assertThat(isReferenceValue(property, "@id/hello")).isFalse()
    assertThat(isReferenceValue(property, "@+id/hello")).isFalse()
    assertThat(isReferenceValue(property, "@android:id/hello")).isFalse()
  }

  @RunsInEdt
  @Test
  fun testResolvedValues() {
    projectRule.fixture.addFileToProject("res/values/values.xml", VALUE_RESOURCES)
    projectRule.fixture.addFileToProject("res/layout/my_layout.xml", "<LinearLayout/>")
    val util = SupportTestUtil(projectRule, createTextView())
    assertThat(resolvedValue(util, NelePropertyType.BOOLEAN, "@bool/useBorder")).isEqualTo("true")
    assertThat(resolvedValue(util, NelePropertyType.COLOR, "@color/opaqueRed")).isEqualTo("#f00")
    assertThat(resolvedValue(util, NelePropertyType.COLOR, "@color/opaqueRedIndirect")).isEqualTo("#f00")
    assertThat(resolvedValue(util, NelePropertyType.COLOR, "@color/translucentRed")).isEqualTo("#80ff0000")
    assertThat(resolvedValue(util, NelePropertyType.DIMENSION, "@dimen/ballRadius")).isEqualTo("30dp")
    assertThat(resolvedValue(util, NelePropertyType.DIMENSION, "@dimen/fontSize")).isEqualTo("16sp")
    assertThat(resolvedValue(util, NelePropertyType.FRACTION, "@fraction/part")).isEqualTo("0.125")
    assertThat(resolvedValue(util, NelePropertyType.ID, "@id/button_ok")).isEqualTo("@id/button_ok")
    assertThat(resolvedValue(util, NelePropertyType.INTEGER, "@integer/records")).isEqualTo("67")
    assertThat(resolvedValue(util, NelePropertyType.STRING, "@string/hello")).isEqualTo("Hello")

    // The following resources will resolve to a file path. Check that we do NOT show the file:
    assertThat(resolvedValue(util, NelePropertyType.COLOR, "@color/text")).isEqualTo("@android:color/primary_text_dark")
    assertThat(resolvedValue(util, NelePropertyType.DRAWABLE, "@drawable/cancel")).isEqualTo("@android:drawable/ic_delete")
    assertThat(resolvedValue(util, NelePropertyType.STYLE, "@style/stdButton")).isEqualTo("@style/stdButton")
    assertThat(resolvedValue(util, NelePropertyType.LAYOUT, "@layout/my_layout")).isEqualTo("@layout/my_layout")
  }

  @RunsInEdt
  @Test
  fun testGetValueWhenDisplayingResolvedValues() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    property.model.showResolvedValues = true
    assertThat(property.value).isEqualTo("Demo String")
    assertThat(property.rawValue).isEqualTo("@string/demo")
    assertThat(property.isReference).isTrue()
  }

  @RunsInEdt
  @Test
  fun testGetSameValueFromMultipleComponents() {
    val util = SupportTestUtil(projectRule, createTextViewAndButtonWithSameTextValue())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    property.model.showResolvedValues = false
    assertThat(property.value).isEqualTo("@string/demo")
    assertThat(property.isReference).isTrue()
    assertThat(property.resolvedValue).isEqualTo("Demo String")
  }

  @RunsInEdt
  @Test
  fun testGetDifferentValueFromMultipleComponents() {
    val util = SupportTestUtil(projectRule, createTextViewAndButtonWithDifferentTextValue())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    assertThat(property.value).isNull()
    assertThat(property.isReference).isFalse()
    assertThat(property.resolvedValue).isNull()
  }

  @RunsInEdt
  @Test
  fun testSetValueOnMultipleComponents() {
    val util = SupportTestUtil(projectRule, createTextViewAndButtonWithDifferentTextValue())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    val components = property.components
    property.value = HELLO_WORLD

    assertThat(property.value).isEqualTo(HELLO_WORLD)
    assertThat(property.isReference).isFalse()
    assertThat(property.resolvedValue).isEqualTo(HELLO_WORLD)
    assertThat(components[0].getAttribute(ANDROID_URI, ATTR_TEXT)).isEqualTo(HELLO_WORLD)
    assertThat(components[1].getAttribute(ANDROID_URI, ATTR_TEXT)).isEqualTo(HELLO_WORLD)
  }

  @RunsInEdt
  @Test
  fun testSetNewToolsValue() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    val design = property.designProperty
    design.value = HELLO_WORLD

    assertThat(design.value).isEqualTo(HELLO_WORLD)
    assertThat(design.isReference).isFalse()
    assertThat(design.resolvedValue).isEqualTo(HELLO_WORLD)
    assertThat(property.model.properties.getOrNull(TOOLS_URI, ATTR_TEXT) != null)
  }

  @RunsInEdt
  @Test
  fun testGetDefaultValue() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT_APPEARANCE, NelePropertyType.STYLE)
    val components = property.components
    val manager = getSceneManager(property)
    manager.putDefaultPropertyValue(components[0], ResourceNamespace.ANDROID, ATTR_TEXT_APPEARANCE, "?attr/textAppearanceSmall")
    waitUntilEventsProcessed(property.model)

    assertThat(property.value).isNull()
    assertThat(property.defaultValue).isEqualTo("@android:style/TextAppearance.Material.Small")
  }

  @RunsInEdt
  @Test
  fun testSetParentTagValue() {
    val util = SupportTestUtil(projectRule, VIEW_MERGE)
    val property = util.makeProperty(TOOLS_URI, ATTR_PARENT_TAG, NelePropertyType.STRING)

    var propertiesGenerated = false
    util.model.addListener(object: PropertiesModelListener<NelePropertyItem> {
      override fun propertiesGenerated(model: PropertiesModel<NelePropertyItem>) {
        propertiesGenerated = true
      }
      override fun propertyValuesChanged(model: PropertiesModel<NelePropertyItem>) {
      }
    })

    property.value = LINEAR_LAYOUT
    assertThat(propertiesGenerated).isTrue()
  }

  @RunsInEdt
  @Test
  fun testToolTipForValue() {
    val util = SupportTestUtil(projectRule, createTextView())
    val components = util.components
    val emptyProperty = util.makeProperty(ANDROID_URI, ATTR_CONTENT_DESCRIPTION, NelePropertyType.STRING)
    val hardcodedProperty = util.makeProperty(ANDROID_URI, ATTR_LAYOUT_WIDTH, NelePropertyType.DIMENSION)
    val referenceProperty = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    val hardcodedFromDefaultProperty = util.makeProperty(ANDROID_URI, ATTR_LINE_SPACING_EXTRA, NelePropertyType.DIMENSION)
    val referenceFromDefaultProperty = util.makeProperty(ANDROID_URI, ATTR_TEXT_SIZE, NelePropertyType.DIMENSION)
    val manager = getSceneManager(hardcodedFromDefaultProperty)
    val keyStroke = KeymapUtil.getShortcutText(ToggleShowResolvedValueAction.SHORTCUT)  // Platform dependent !!!
    manager.putDefaultPropertyValue(components[0], ResourceNamespace.ANDROID, ATTR_LINE_SPACING_EXTRA, "16sp")
    manager.putDefaultPropertyValue(components[0], ResourceNamespace.ANDROID, ATTR_TEXT_SIZE, "@dimen/text_size_button_material")
    waitUntilEventsProcessed(util.model)

    assertThat(emptyProperty.tooltipForValue).isEmpty()
    assertThat(hardcodedProperty.tooltipForValue).isEmpty()
    assertThat(referenceProperty.tooltipForValue).isEqualTo("\"@string/demo\" = \"Demo String\" ($keyStroke)")
    assertThat(hardcodedFromDefaultProperty.tooltipForValue).isEqualTo("[default] \"16sp\"")
    assertThat(referenceFromDefaultProperty.tooltipForValue).isEqualTo("[default] \"14sp\"")
  }

  // Completions are not run on the EDT
  @Test
  fun testCompletion() {
    val util = SupportTestUtil(projectRule, createTextView())
    val text = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    val values = text.editingSupport.completion()
    assertThat(values.size).isAtLeast(25)
    assertThat(values.filter { it.startsWith("@string/") }).containsExactly("@string/demo", "@string/design").inOrder()
    assertThat(values).containsAllOf("@android:string/yes", "@android:string/no", "@android:string/cancel")
  }

  // Completions are not run on the EDT
  @Test
  fun testIdCompletion() {
    val util = SupportTestUtil(projectRule, createMultipleComponents())
    util.components.retainAll(listOf(util.components.first())) // Select the first components for the property
    val toEndOf = util.makeProperty(ANDROID_URI, ATTR_LAYOUT_TO_END_OF, NelePropertyType.ID)
    val values = toEndOf.editingSupport.completion()
    assertThat(values).containsExactly("@id/button1", "@id/text2", "@id/button2").inOrder()
  }

  // Completions are not run on the EDT
  @Test
  fun testFontCompletion() {
    projectRule.fixture.copyFileToProject("fonts/customfont.ttf", "res/font/customfont.ttf")
    val util = SupportTestUtil(projectRule, createTextView())
    val font = util.makeProperty(ANDROID_URI, ATTR_FONT_FAMILY, NelePropertyType.FONT)
    val values = font.editingSupport.completion()
    val expected = mutableListOf("@font/customfont")
    expected.addAll(AndroidDomUtil.AVAILABLE_FAMILIES)
    assertThat(values).containsExactlyElementsIn(expected)
  }

  @Test
  fun testbackgroundCompletion() {
    projectRule.fixture.addFileToProject("res/values/values.xml", VALUE_RESOURCES)
    val util = SupportTestUtil(projectRule, createTextView())
    val background = util.makeProperty(ANDROID_URI, ATTR_BACKGROUND, NelePropertyType.DRAWABLE)
    val values = background.editingSupport.completion()
    val set = HashSet(values)

    // There must be no duplicates
    assertThat(values.size).isEqualTo(set.size)

    // Check that local resources are shown before framework resources
    val localValue = values.indexOf("@drawable/cancel")
    val firstFrameworkValue = values.indexOfFirst { it.startsWith("@android:") }
    assertThat(localValue).isAtLeast(0)
    assertThat(firstFrameworkValue).isAtLeast(0)
    assertThat(localValue).isLessThan(firstFrameworkValue)
  }

  @RunsInEdt
  @Test
  fun testParentTagCompletion() {
    val util = SupportTestUtil(projectRule, VIEW_MERGE)
    val text = util.makeProperty(TOOLS_URI, ATTR_PARENT_TAG, NelePropertyType.STRING)
    val values = text.editingSupport.completion()
    assertThat(values).containsAllOf(LINEAR_LAYOUT, ABSOLUTE_LAYOUT, FRAME_LAYOUT)
  }

  @RunsInEdt
  @Test
  fun testColorValidation() {
    projectRule.fixture.addFileToProject("res/values/values.xml", VALUE_RESOURCES)
    val util = SupportTestUtil(projectRule, createTextView())
    val color = util.makeProperty(ANDROID_URI, ATTR_TEXT_COLOR, NelePropertyType.COLOR_STATE_LIST)
    assertThat(color.editingSupport.validation("")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(color.editingSupport.validation("#FF00FF")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(color.editingSupport.validation("?android:attr/colorPrimary")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(color.editingSupport.validation("@null")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(color.editingSupport.validation("@android:color/holo_blue_bright")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(color.editingSupport.validation("@color/translucentRed")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(color.editingSupport.validation("@android:drawable/btn_minus")).isEqualTo(
      Pair(ERROR, "Unexpected resource type: 'drawable' expected: color"))
    assertThat(color.editingSupport.validation("#XYZ")).isEqualTo(Pair(ERROR, "Invalid color value: '#XYZ'"))
    assertThat(color.editingSupport.validation("?android:attr/no_color")).isEqualTo(
      Pair(ERROR, "Cannot resolve theme reference: 'android:attr/no_color'"))
    assertThat(color.editingSupport.validation("@hello/hello")).isEqualTo(Pair(ERROR, "Unknown resource type hello"))
    assertThat(color.editingSupport.validation("@string/hello")).isEqualTo(
      Pair(ERROR, "Unexpected resource type: 'string' expected: color"))
    assertThat(color.editingSupport.validation("@android:color/no_color")).isEqualTo(Pair(ERROR, "Cannot resolve symbol: 'no_color'"))
    assertThat(color.editingSupport.validation("@color/no_color")).isEqualTo(Pair(ERROR, "Cannot resolve symbol: 'no_color'"))
  }

  @RunsInEdt
  @Test
  fun testDrawableValidation() {
    projectRule.fixture.addFileToProject("res/values/values.xml", VALUE_RESOURCES)
    projectRule.fixture.copyFileToProject("mipmap/mipmap-hdpi/ic_launcher.png", "res/mipmap-hdpi/ic_launcher.png")
    projectRule.fixture.copyFileToProject("mipmap/mipmap-mdpi/ic_launcher.png", "res/mipmap-mdpi/ic_launcher.png")
    projectRule.fixture.copyFileToProject("mipmap/mipmap-xhdpi/ic_launcher.png", "res/mipmap-xhdpi/ic_launcher.png")
    val util = SupportTestUtil(projectRule, createImageView())
    val srcCompat = util.makeProperty(ANDROID_URI, ATTR_SRC_COMPAT, NelePropertyType.DRAWABLE)
    assertThat(srcCompat.editingSupport.validation("")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("#FF00FF")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("?android:attr/colorPrimary")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("@null")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("@android:color/holo_blue_bright")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("@color/translucentRed")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("@android:drawable/btn_minus")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("#XYZ")).isEqualTo(Pair(ERROR, "Invalid color value: '#XYZ'"))
    assertThat(srcCompat.editingSupport.validation("?android:attr/no_color")).isEqualTo(
      Pair(ERROR, "Cannot resolve theme reference: 'android:attr/no_color'"))
    assertThat(srcCompat.editingSupport.validation("@hello/hello")).isEqualTo(Pair(ERROR, "Unknown resource type hello"))
    assertThat(srcCompat.editingSupport.validation("@string/hello")).isEqualTo(
      Pair(ERROR, "Unexpected resource type: 'string' expected one of: color, drawable, mipmap"))
    assertThat(srcCompat.editingSupport.validation("@android:color/no_color")).isEqualTo(Pair(ERROR, "Cannot resolve symbol: 'no_color'"))
    assertThat(srcCompat.editingSupport.validation("@color/no_color")).isEqualTo(Pair(ERROR, "Cannot resolve symbol: 'no_color'"))
    assertThat(srcCompat.editingSupport.validation("@mipmap/ic_launcher")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(srcCompat.editingSupport.validation("@mipmap/ic_not_found")).isEqualTo(Pair(ERROR, "Cannot resolve symbol: 'ic_not_found'"))
  }

  @Test
  fun testEnumValidation() {
    val util = SupportTestUtil(projectRule, createTextView())
    val visibility = util.makeProperty(ANDROID_URI, ATTR_VISIBILITY, NelePropertyType.ENUM)
    assertThat(visibility.editingSupport.validation("")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(visibility.editingSupport.validation("visible")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(visibility.editingSupport.validation("invisible")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(visibility.editingSupport.validation("gone")).isEqualTo(EDITOR_NO_ERROR)
    assertThat(visibility.editingSupport.validation("blue")).isEqualTo(Pair(ERROR, "Invalid value: 'blue'"))
  }

  @RunsInEdt
  @Test
  fun testSampleDataValidation() {
    val util = SupportTestUtil(projectRule, createTextView())
    val src = util.makeProperty(ANDROID_URI, ATTR_SRC, NelePropertyType.DRAWABLE)
    assertThat(src.editingSupport.validation("@tools:sample/avatars[1]")).isEqualTo(EDITOR_NO_ERROR)
  }

  @RunsInEdt
  @Test
  fun testColorIconOfBackgroundAttribute() {
    val util = SupportTestUtil(projectRule, createImageView())
    val background = util.makeProperty(ANDROID_URI, ATTR_BACKGROUND, NelePropertyType.DRAWABLE)
    assertThat(background.colorButton?.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Extras.PIPETTE)

    background.value = "@drawable/non-existent-drawable"
    assertThat(background.colorButton?.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Properties.IMAGE_PICKER)

    background.value = "@color/non-existent-color"
    assertThat(background.colorButton?.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Extras.PIPETTE)
  }

  @RunsInEdt
  @Test
  fun testColorIconOfSrcAttribute() {
    val util = SupportTestUtil(projectRule, createImageView())
    val src = util.makeProperty(ANDROID_URI, ATTR_SRC, NelePropertyType.DRAWABLE)
    src.value = null
    assertThat(src.colorButton?.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Properties.IMAGE_PICKER)

    src.value = "@color/non-existent-color"
    assertThat(src.colorButton?.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Extras.PIPETTE)

    src.value = "@drawable/non-existent-drawable"
    assertThat(src.colorButton?.actionIcon).isEqualTo(StudioIcons.LayoutEditor.Properties.IMAGE_PICKER)
  }

  @RunsInEdt
  @Test
  fun testBrowse() {
    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT_APPEARANCE, NelePropertyType.STYLE)
    property.value = "@android:style/TextAppearance.Material.Display2"

    val fileManager = mock(FileEditorManager::class.java)
    componentStack!!.registerComponentImplementation(FileEditorManager::class.java, fileManager)
    val file = ArgumentCaptor.forClass(OpenFileDescriptor::class.java)
    Mockito.`when`(fileManager.openEditor(ArgumentMatchers.any(OpenFileDescriptor::class.java), ArgumentMatchers.anyBoolean()))
      .thenReturn(listOf(mock(FileEditor::class.java)))

    property.helpSupport.browse()
    Mockito.verify(fileManager).openEditor(file.capture(), ArgumentMatchers.eq(true))
    val descriptor = file.value
    assertThat(descriptor.file.name).isEqualTo("styles_material.xml")
    assertThat(findLineAtOffset(descriptor.file, descriptor.offset)).isEqualTo("<style name=\"TextAppearance.Material.Display2\">")
  }

  @RunsInEdt
  @Test
  fun testSetValueIgnoredDuringUndo() {
    val undoManager = mock(UndoManagerImpl::class.java)
    componentStack!!.registerComponentImplementation(UndoManager::class.java, undoManager)
    `when`(undoManager.isUndoInProgress).thenReturn(true)

    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    property.value = HELLO_WORLD
    assertThat(property.value).isEqualTo("@string/demo")
  }

  @RunsInEdt
  @Test
  fun testSetValueIgnoredDuringRedo() {
    val undoManager = mock(UndoManagerImpl::class.java)
    componentStack!!.registerComponentImplementation(UndoManager::class.java, undoManager)
    `when`(undoManager.isRedoInProgress).thenReturn(true)

    val util = SupportTestUtil(projectRule, createTextView())
    val property = util.makeProperty(ANDROID_URI, ATTR_TEXT, NelePropertyType.STRING)
    property.value = HELLO_WORLD
    assertThat(property.value).isEqualTo("@string/demo")
  }

  private fun createTextView(): ComponentDescriptor =
    ComponentDescriptor(TEXT_VIEW)
          .withAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, "wrap_content")
          .withAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, "wrap_content")
          .withAttribute(ANDROID_URI, ATTR_TEXT, "@string/demo")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "@string/design")

  private fun createImageView(): ComponentDescriptor =
    ComponentDescriptor(IMAGE_VIEW)
        .withAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, "wrap_content")
        .withAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, "wrap_content")
        .withAttribute(ANDROID_URI, ATTR_SRC, "@tools:sample/avatars[1]")

  private fun createTextViewWithHardcodedValue(): ComponentDescriptor =
    ComponentDescriptor(TEXT_VIEW)
        .withAttribute(ANDROID_URI, ATTR_TEXT, "Hardcoded string")
        .withAttribute(TOOLS_URI, ATTR_TEXT, "Hardcoded design string")

  private fun createTextViewWithTextColor(textColor: String): ComponentDescriptor =
    ComponentDescriptor(TEXT_VIEW)
        .withAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, "wrap_content")
        .withAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, "wrap_content")
        .withAttribute(ANDROID_URI, ATTR_TEXT_COLOR, textColor)

  private fun createTextViewAndButtonWithSameTextValue(): ComponentDescriptor =
    ComponentDescriptor(LINEAR_LAYOUT)
      .withBounds(0, 0, 2000, 2000)
      .matchParentWidth()
      .matchParentHeight()
      .children(
        ComponentDescriptor(TEXT_VIEW)
          .withBounds(0, 0, 200, 20)
          .withAttribute(ANDROID_URI, ATTR_TEXT, "@string/demo")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "@string/design"),
        ComponentDescriptor(BUTTON)
          .withBounds(0, 20, 200, 20)
          .withAttribute(ANDROID_URI, ATTR_TEXT, "@string/demo")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "@string/design"))

  private fun createTextViewAndButtonWithDifferentTextValue(): ComponentDescriptor =
    ComponentDescriptor(LINEAR_LAYOUT)
      .withBounds(0, 0, 2000, 2000)
      .matchParentWidth()
      .matchParentHeight()
      .children(
        ComponentDescriptor(TEXT_VIEW)
          .withBounds(0, 0, 200, 20)
          .withAttribute(ANDROID_URI, ATTR_TEXT, "@string/demo")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "@string/design"),
        ComponentDescriptor(BUTTON)
          .withBounds(0, 20, 200, 20)
          .withAttribute(ANDROID_URI, ATTR_TEXT, "other")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "something"))

  private fun createMultipleComponents(): ComponentDescriptor =
    ComponentDescriptor(RELATIVE_LAYOUT)
      .withBounds(0, 0, 2000, 2000)
      .matchParentWidth()
      .matchParentHeight()
      .children(
        ComponentDescriptor(TEXT_VIEW)
          .withBounds(0, 0, 200, 20)
          .id(NEW_ID_PREFIX + "text1")
          .withAttribute(ANDROID_URI, ATTR_TEXT, "@string/demo")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "@string/design"),
        ComponentDescriptor(BUTTON)
          .withBounds(0, 20, 200, 20)
          .id(NEW_ID_PREFIX + "button1")
          .withAttribute(ANDROID_URI, ATTR_TEXT, "@string/demo")
          .withAttribute(TOOLS_URI, ATTR_TEXT, "@string/design"),
        ComponentDescriptor(TEXT_VIEW)
          .withBounds(0, 40, 200, 20)
          .id(NEW_ID_PREFIX + "text2")
          .withAttribute(ANDROID_URI, ATTR_TEXT, "demo"),
        ComponentDescriptor(BUTTON)
          .withBounds(0, 60, 200, 20)
          .id(NEW_ID_PREFIX + "button2")
          .withAttribute(ANDROID_URI, ATTR_TEXT, "other"))

  private fun isReferenceValue(property: NelePropertyItem, value: String): Boolean {
    property.value = value
    return property.isReference
  }

  private fun resolvedValue(util: SupportTestUtil, type: NelePropertyType, value: String): String? {
    val property = util.makeProperty(ANDROID_URI, "name", type)
    property.value = value
    return property.resolvedValue
  }

  @Language("XML")
  private val VALUE_RESOURCES = """<?xml version="1.0" encoding="utf-8"?>
    <resources>
      <bool name="useBorder">true</bool>
      <color name="opaqueRed">#f00</color>
      <color name="translucentRed">#80ff0000</color>
      <color name="opaqueRedIndirect">@color/opaqueRed</color>
      <dimen name="ballRadius">30dp</dimen>
      <dimen name="fontSize">16sp</dimen>
      <fraction name="part">0.125</fraction>
      <item type="id" name="button_ok" />
      <integer name="records">67</integer>
      <string name="hello">Hello</string>

      <color name="text">@android:color/primary_text_dark</color>
      <drawable name="cancel">@android:drawable/ic_delete</drawable>
      <style name="stdButton" parent="@android:style/TextAppearance.Material.Widget.Button"/>
    </resources>
  """.trimIndent()

  private fun findLineAtOffset(file: VirtualFile, offset: Int): String {
    val text = String(file.contentsToByteArray(), Charsets.UTF_8)
    val line = StringUtil.offsetToLineColumn(text, offset)
    val lineText = text.substring(offset - line.column, text.indexOf('\n', offset))
    return lineText.trim()
  }

  private fun getSceneManager(property: NelePropertyItem): SyncLayoutlibSceneManager {
    return property.model.surface!!.currentSceneView!!.sceneManager as SyncLayoutlibSceneManager
  }
}
