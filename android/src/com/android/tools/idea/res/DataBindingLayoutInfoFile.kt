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
package com.android.tools.idea.res

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

/**
 * This class represents an XML file that hosts data binding logic - in other words, in addition
 * to being a regular XML file, it is also aware that it owns a data binding class.
 *
 * Additional notes:
 *
 * We create a custom [PsiFile] implementation for [DataBindingLayoutInfo] files, to work around
 * the fact that data binding otherwise causes the IntelliJ code coverage runner to crash.
 *
 * The reason this happens is that data binding code is somewhat special, since binding classes are
 * generated from an XML file (e.g. `activity_main.xml` generates `ActivityMainBinding`). These
 * generated binding classes point back at the XML as its parent file.
 *
 * The IntelliJ code coverage runner, however, assumes that every class it iterates over belongs
 * to a containing file which is a [PsiClassOwner] - in other words, a class belongs to a file that
 * contains one or more classes. This would usually make sense, such as a Java class living inside
 * a Java file, except in our case, XML files are NOT owners of classes.
 *
 * Therefore, we create a special-case file which is REALLY just an XML file that also implements
 * [PsiClassOwner] to indicate the fact that this XML file does, indeed, own a class.
 *
 * For even more context, see also https://issuetracker.google.com/120561619
 */
class DataBindingLayoutInfoFile(private val info: DataBindingLayoutInfo) : PsiFile by info.psiFile, PsiClassOwner {
  override fun getContainingFile(): PsiFile {
    // Return ourselves instead of delegating to the target XML file, since we're the containing
    // file that also implements PsiClassOwner
    return this
  }

  override fun getClasses(): Array<PsiClass> = arrayOf(info.psiClass)

  override fun getPackageName(): String {
    return info.psiClass.qualifiedName?.substringBeforeLast('.') ?: ""
  }

  override fun setPackageName(packageName: String?) {
    throw IncorrectOperationException("Cannot set package name for generated data binding classes")
  }
}