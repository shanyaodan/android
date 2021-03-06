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
package com.android.tools.idea.res;

import com.android.ide.common.resources.DataBindingResourceType;
import com.android.utils.HashCodes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * An interface for data binding related information that can be extracted from a layout xml file.
 */
public interface DataBindingLayoutInfo extends ModificationTracker {
  AndroidFacet getFacet();

  String getClassName();

  String getPackageName();

  Project getProject();

  String getQualifiedName();

  PsiElement getNavigationElement();

  PsiFile getPsiFile();

  PsiClass getPsiClass();

  void setPsiClass(PsiClass psiClass);

  /**
   * Whether this info is extracted from a singleton layout file or merged layout files.
   */
  boolean isMerged();

  /**
   * Additional info if a target layout file has additional configurations (e.g. for other
   * resolutions or orientations).
   */
  @Nullable
  DataBindingLayoutInfo getMergedInfo();

  @NotNull
  Map<String, PsiDataBindingResourceItem> getItems(@NotNull DataBindingResourceType type);

  @NotNull
  List<ViewWithId> getViewsWithIds();

  @Nullable
  Module getModule();

  @Override
  long getModificationCount();

  @Nullable
  default String resolveImport(@NotNull String nameOrAlias) {
    Map<String, PsiDataBindingResourceItem> imports = getItems(DataBindingResourceType.IMPORT);
    PsiDataBindingResourceItem importItem = imports.get(nameOrAlias);
    return importItem == null ? null : importItem.getTypeDeclaration();
  }

  class ViewWithId {
    @NotNull
    public final String name;
    @NotNull
    public final XmlTag tag;

    public ViewWithId(@NotNull String name, @NotNull XmlTag tag) {
      this.name = name;
      this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ViewWithId)) return false;
      ViewWithId id = (ViewWithId)o;
      return name.equals(id.name) && tag.equals(id.tag);
    }

    @Override
    public int hashCode() {
      return HashCodes.mix(name.hashCode(), tag.hashCode());
    }

    @Override
    public String toString() {
      return "ViewWithId{" +
             "name='" + name + '\'' +
             ", tag=" + tag +
             '}';
    }
  }
}
