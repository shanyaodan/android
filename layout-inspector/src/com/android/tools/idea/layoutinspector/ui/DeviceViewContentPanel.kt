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
package com.android.tools.idea.layoutinspector.ui

import com.android.tools.idea.layoutinspector.LayoutInspector
import com.android.tools.idea.layoutinspector.model.InspectorModel
import com.android.tools.idea.layoutinspector.model.InspectorView
import com.intellij.util.ui.UIUtil
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import javax.swing.JPanel
import kotlin.properties.Delegates

class DeviceViewContentPanel(layoutInspector: LayoutInspector, initialScale: Double, initialMode: DeviceViewPanel.ViewMode) : JPanel() {

  private val inspectorModel = layoutInspector.layoutInspectorModel
  internal var model = DeviceViewPanelModel(inspectorModel)

  internal var scale by Delegates.observable(initialScale) { _, _, _ -> repaint() }

  internal var viewMode by Delegates.observable(initialMode) { _, _, newMode ->
    if (newMode == DeviceViewPanel.ViewMode.FIXED) {
      model.resetRotation()
      repaint()
    }
  }

  internal var drawBorders by Delegates.observable(true) { _, _, _ -> repaint() }

  private val HQ_RENDERING_HINTS = mapOf(
    RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
    RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
    RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BILINEAR,
    RenderingHints.KEY_STROKE_CONTROL to RenderingHints.VALUE_STROKE_PURE
  )

  init {
    layoutInspector.modelChangeListeners.add(::modelChanged)
    inspectorModel.modificationListeners.add(::modelChanged)
    inspectorModel.selectionListeners.add(::selectionChanged)
    val mouseListener = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        inspectorModel.selection = model.findTopRect((e.x - size.width / 2.0) / scale,
                                                     (e.y - size.height / 2.0) / scale)
        repaint()
      }
    }
    addMouseListener(mouseListener)
    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        repaint()
      }
    })

    val listener = object : MouseAdapter() {
      private var x = 0
      private var y = 0

      override fun mousePressed(e: MouseEvent) {
        x = e.x
        y = e.y
      }

      override fun mouseDragged(e: MouseEvent) {
        var xRotation = 0.0
        var yRotation = 0.0
        if (viewMode != DeviceViewPanel.ViewMode.FIXED) {
          xRotation = (e.x - x) * 0.001
          x = e.x
        }
        if (viewMode == DeviceViewPanel.ViewMode.XY) {
          yRotation = (e.y - y) * 0.001
          y = e.y
        }
        if (xRotation != 0.0 || yRotation != 0.0) {
          model.rotate(xRotation, yRotation)
        }
        repaint()
      }
    }
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  override fun paint(g: Graphics) {
    val g2d = g as? Graphics2D ?: return
    g2d.color = background
    g2d.fillRect(0, 0, width, height)
    g2d.setRenderingHints(HQ_RENDERING_HINTS)
    g2d.translate(size.width / 2.0, size.height / 2.0)
    g2d.scale(scale, scale)
    model.hitRects.forEach { (rect, transform, view) ->
      drawView(g2d, view, rect, transform)
    }
  }

  override fun getPreferredSize() = Dimension((model.maxWidth * scale + 50).toInt(), (model.maxHeight * scale + 50).toInt())

  private fun drawView(g: Graphics,
                       view: InspectorView,
                       rect: Shape,
                       transform: AffineTransform) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHints(HQ_RENDERING_HINTS)
    val selection = inspectorModel.selection
    if (drawBorders) {
      if (view == selection) {
        g2.color = Color.RED
        g2.stroke = BasicStroke(3f)
      }
      else {
        g2.color = Color.BLUE
        g2.stroke = BasicStroke(1f)
      }
      g2.draw(rect)
    }

    g2.transform = g2.transform.apply { concatenate(transform) }

    val bufferedImage = view.image
    if (bufferedImage != null) {
      val composite = g2.composite
      if (selection != null && view != selection) {
        g2.composite = AlphaComposite.SrcOver.derive(0.6f)
      }
      UIUtil.drawImage(g2, bufferedImage, view.x, view.y, null)
      g2.composite = composite
    }
    if (drawBorders && view == selection) {
      g2.color = Color.BLACK
      g2.font = g2.font.deriveFont(20f)
      g2.drawString(view.type, view.x + 5, view.y + 25)
    }
  }

  private fun selectionChanged(old: InspectorView?, new: InspectorView?) {
    repaint()
  }

  private fun modelChanged(old: InspectorView?, new: InspectorView?) {
    model.refresh()
    repaint()
  }

  private fun modelChanged(old: InspectorModel, new: InspectorModel) {
    old.selectionListeners.remove(::selectionChanged)
    new.selectionListeners.add(::selectionChanged)
    old.modificationListeners.remove(::modelChanged)
    new.modificationListeners.add(::modelChanged)

    model = DeviceViewPanelModel(new)
    repaint()
  }
}