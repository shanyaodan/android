/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.lint;

import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.SuppressionUtilCore;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.android.SdkConstants.*;
import static org.jetbrains.android.inspections.lint.AndroidLintInspectionBase.LINT_INSPECTION_PREFIX;

/** Intention for adding a {@code @SuppressLint} annotation on the given element for the given id */
public class SuppressLintIntentionAction implements IntentionAction, Iconable {
  private static final String NO_INSPECTION_PREFIX = "//" + SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME + " ";
  private final String myId;
  private final PsiElement myElement;
  private Issue myIssue;

  public SuppressLintIntentionAction(String id, PsiElement element) {
    myId = id;
    myElement = element;
  }

  public SuppressLintIntentionAction(Issue issue, PsiElement element) {
    this(issue.getId(), element);
    myIssue = issue;
  }

  @Override
  public Icon getIcon(@IconFlags int flags) {
    return AllIcons.Actions.Cancel;
  }

  @NotNull
  @Override
  public String getText() {
    String id = getLintId(myId);
    final PsiFile file = PsiTreeUtil.getParentOfType(myElement, PsiFile.class);
    if (file == null) {
      return "";
    } else if (file instanceof XmlFile) {
      return AndroidBundle.message("android.lint.fix.suppress.lint.api.attr", id);
    } else if (file instanceof PsiJavaFile) {
      return AndroidBundle.message("android.lint.fix.suppress.lint.api.annotation", id);
    } else if (file instanceof GroovyFile) {
      return "Suppress: Add //noinspection " + id;
    } else {
      return "";
    }
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    FileType type = file.getFileType();
    return type == StdFileTypes.JAVA || type == StdFileTypes.XML || type == GroovyFileType.GROOVY_FILE_TYPE;
  }

  @Override
  public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiFile file) throws IncorrectOperationException {
    if (file instanceof XmlFile) {
      final XmlTag element = PsiTreeUtil.getParentOfType(myElement, XmlTag.class);
      if (element == null) {
        return;
      }

      String lintId = getLintId(myId);
      addSuppressAttribute(project, (XmlFile) file, element, lintId);
    } else if (file instanceof PsiJavaFile) {
      final PsiModifierListOwner container =
        PsiTreeUtil.getParentOfType(myElement, PsiModifierListOwner.class);
      if (container == null) {
        return;
      }

      final PsiModifierList modifierList = container.getModifierList();
      if (modifierList != null) {

        String lintId = getLintId(myId);
        addSuppressAnnotation(project, container, container, lintId);
      }
    } else if (file instanceof GroovyFile) {
      Document document = PsiDocumentManager.getInstance(myElement.getProject()).getDocument(file);
      if (document != null) {
        int offset = myElement.getTextOffset();
        int line = document.getLineNumber(offset);
        int lineStart = document.getLineStartOffset(line);
        if (lineStart > 0) {
          int prevLineStart = document.getLineStartOffset(line - 1);
          int prevLineEnd = document.getLineEndOffset(line - 1);
          String prevLine = document.getText(new TextRange(prevLineStart, prevLineEnd));
          int index = prevLine.indexOf(NO_INSPECTION_PREFIX);
          if (index != -1) {
            document.insertString(prevLineStart + index + NO_INSPECTION_PREFIX.length(), getLintId(myId) + ",");
            return;
          }
        }
        String linePrefix = document.getText(new TextRange(lineStart, offset));
        int nonSpace = 0;
        for (; nonSpace < linePrefix.length(); nonSpace++) {
          if (!Character.isWhitespace(linePrefix.charAt(nonSpace))) {
            break;
          }
        }
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        document.insertString(lineStart + nonSpace, NO_INSPECTION_PREFIX + getLintId(myId) + "\n" + linePrefix.substring(0, nonSpace));
      }
    } else if (file instanceof PsiBinaryFile) {
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        File binaryFile = VfsUtilCore.virtualToIoFile(virtualFile);

        // Can't suppress lint checks inside a binary file (typically an icon): use
        // the lint XML facility instead
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        if (module != null) {
          //LintIdeRequest
          File dir = LintIdeProject.getLintProjectDirectory(module, AndroidFacet.getInstance(module));
          if (dir != null) {
            LintIdeClient client = new LintIdeClient(project);
            com.android.tools.lint.detector.api.Project lintProject = client.getProject(dir, dir);
            Configuration configuration = client.getConfiguration(lintProject, null);
            Issue issue = getIssue();
            if (issue != null) {
              configuration.ignore(issue, binaryFile);
            }
          }
        }
      }
    }
  }

  @Nullable
  private Issue getIssue() {
    if (myIssue == null) {
      myIssue = new LintIdeIssueRegistry().getIssue(myId);
    }

    return myIssue;
  }

  static String getLintId(String intentionId) {
    String lintId = intentionId;
    lintId = StringUtil.trimStart(lintId, LINT_INSPECTION_PREFIX);

    return lintId;
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private static void addSuppressAttribute(final Project project,
                                           final XmlFile file,
                                           final XmlTag element,
                                           final String id) throws IncorrectOperationException {
    XmlAttribute attribute = element.getAttribute(ATTR_IGNORE, TOOLS_URI);
    String value;
    if (attribute == null) {
      value = id;
    } else {
      List<String> ids = new ArrayList<>();
      for (String existing : Splitter.on(',').trimResults().split(attribute.getValue())) {
        if (!existing.equals(id)) {
          ids.add(existing);
        }
      }
      ids.add(id);
      Collections.sort(ids);
      value = Joiner.on(',').join(ids);
    }
    AndroidResourceUtil.ensureNamespaceImported(file, TOOLS_URI, null);
    element.setAttribute(ATTR_IGNORE, TOOLS_URI, value);
  }

  // Based on the equivalent code in com.intellij.codeInsight.daemon.impl.actions.SuppressFix
  // to add @SuppressWarnings annotations

  private static void addSuppressAnnotation(final Project project,
                                           final PsiElement container,
                                           final PsiModifierListOwner modifierOwner,
                                           final String id) throws IncorrectOperationException {
    PsiAnnotation annotation = AnnotationUtil.findAnnotation(modifierOwner, FQCN_SUPPRESS_LINT);
    final PsiAnnotation newAnnotation = createNewAnnotation(project, container, annotation, id);
    if (newAnnotation != null) {
      if (annotation != null && annotation.isPhysical()) {
        annotation.replace(newAnnotation);
      }
      else {
        final PsiNameValuePair[] attributes = newAnnotation.getParameterList().getAttributes();
        //noinspection ConstantConditions
        new AddAnnotationFix(FQCN_SUPPRESS_LINT, modifierOwner, attributes).invoke(project, null /*editor*/,
                                                                                   container.getContainingFile());
      }
    }
  }

  @Nullable
  private static PsiAnnotation createNewAnnotation(@NotNull final Project project,
                                                   @NotNull final PsiElement container,
                                                   @Nullable final PsiAnnotation annotation,
                                                   @NotNull final String id) {
    if (annotation != null) {
      final String currentSuppressedId = "\"" + id + "\"";
      String annotationText = annotation.getText();
      if (!annotationText.contains("{")) {
        final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        if (attributes.length == 1) {
          final String suppressedWarnings = attributes[0].getText();
          if (suppressedWarnings.contains(currentSuppressedId)) return null;
          return JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(
            "@" + FQCN_SUPPRESS_LINT  + "({" + suppressedWarnings + ", " + currentSuppressedId + "})", container);

        }
      }
      else {
        final int curlyBraceIndex = annotationText.lastIndexOf('}');
        if (curlyBraceIndex > 0) {
          final String oldSuppressWarning = annotationText.substring(0, curlyBraceIndex);
          if (oldSuppressWarning.contains(currentSuppressedId)) return null;
          return JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(
            oldSuppressWarning + ", " + currentSuppressedId + "})", container);
        }
      }
    }
    else {
      return JavaPsiFacade.getInstance(project).getElementFactory()
        .createAnnotationFromText("@" + FQCN_SUPPRESS_LINT  + "(\"" + id + "\")", container);
    }
    return null;
  }
}
