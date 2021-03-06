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
package com.android.tools.idea.common.lint;

import com.android.ide.common.rendering.api.ResourceReference;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.NlModel;
import com.android.tools.idea.common.surface.DesignSurface;
import com.android.tools.idea.common.surface.SceneView;
import com.android.tools.lint.detector.api.Issue;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeHighlighting.HighlightingPass;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import java.lang.ref.WeakReference;
import org.jetbrains.android.inspections.lint.AndroidLintExternalAnnotator;
import org.jetbrains.android.inspections.lint.AndroidLintInspectionBase;
import org.jetbrains.android.inspections.lint.AndroidLintUtil;
import org.jetbrains.android.inspections.lint.ProblemData;
import org.jetbrains.android.inspections.lint.State;
import org.jetbrains.annotations.NotNull;

public class LintHighlightingPass implements HighlightingPass {
  private final WeakReference<DesignSurface> mySurfaceRef;
  private LintAnnotationsModel myLintAnnotationsModel;

  /**
   * @param surface the surface to add the lint annotations to. This class will keep a {@link WeakReference} to the
   *                surface so it won't stop it from being disposed.
   */
  public LintHighlightingPass(@NotNull DesignSurface surface) {
    mySurfaceRef = new WeakReference<>(surface);
  }

  @Override
  public void collectInformation(@NotNull ProgressIndicator progress) {
    myLintAnnotationsModel = null;
    DesignSurface surface = mySurfaceRef.get();
    if (surface == null) {
      // The surface is gone, no need to keep going
      return;
    }

    SceneView sceneView = surface.getCurrentSceneView();
    if (sceneView == null) {
      return;
    }

    myLintAnnotationsModel = getAnnotations(sceneView.getModel(), progress);
  }

  @Override
  public void applyInformationToEditor() {
    DesignSurface surface = mySurfaceRef.get();
    if (surface == null) {
      // The surface is gone, no need to keep going
      return;
    }

    SceneView sceneView = surface.getCurrentSceneView();
    if (sceneView == null || myLintAnnotationsModel == null) {
      return;
    }

    sceneView.getModel().setLintAnnotationsModel(myLintAnnotationsModel);
    surface.setLintAnnotationsModel(myLintAnnotationsModel);
    // Ensure that the layers are repainted to reflect the latest model
    // (updating the lint annotations associated with a model doesn't actually rev the model
    // version.)
    sceneView.getSurface().repaint();
  }

  @NotNull
  private static LintAnnotationsModel getAnnotations(@NotNull NlModel model, @NotNull ProgressIndicator progress) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    LintAnnotationsModel lintModel = new LintAnnotationsModel();

    XmlFile xmlFile = model.getFile();

    AndroidLintExternalAnnotator annotator = new AndroidLintExternalAnnotator();
    State state = annotator.collectInformation(xmlFile);
    // TODO: Separate analytics mode here?
    if (state != null) {
      state = annotator.doAnnotate(state);
    }

    if (state == null) {
      return lintModel;
    }

    for (ProblemData problemData : state.getProblems()) {
      if (progress.isCanceled()) {
        break;
      }

      TextRange range = problemData.getTextRange();
      final PsiElement startElement = xmlFile.findElementAt(range.getStartOffset());
      final PsiElement endElement = xmlFile.findElementAt(range.getEndOffset());
      if (startElement == null || endElement == null) {
        continue;
      }

      NlComponent component = model.findViewByPsi(startElement);
      if (component == null) {
        continue;
      }

      ResourceReference attribute = model.findAttributeByPsi(startElement);
      AttributeKey attributeKey =
        attribute != null ? new AttributeKey(component, attribute.getNamespace().getXmlNamespaceUri(), attribute.getName()) : null;

      Issue issue = problemData.getIssue();
      Pair<AndroidLintInspectionBase, HighlightDisplayLevel> pair =
        AndroidLintUtil.getHighlightLevelAndInspection(xmlFile.getProject(), issue, xmlFile);
      if (pair == null) {
        continue;
      }

      AndroidLintInspectionBase inspection = pair.getFirst();
      if (inspection == null) {
        continue;
      }

      HighlightDisplayLevel level = pair.getSecond();
      HighlightDisplayKey key = HighlightDisplayKey.find(inspection.getShortName());
      if (key == null) {
        continue;
      }

      lintModel.addIssue(component, attributeKey, issue, problemData.getMessage(), inspection, level,
                         startElement, endElement, problemData.getQuickfixData());
    }

    return lintModel;
  }
}
