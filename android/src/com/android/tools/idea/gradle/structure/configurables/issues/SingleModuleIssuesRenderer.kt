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
package com.android.tools.idea.gradle.structure.configurables.issues

import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.model.PsIssue
import com.android.tools.idea.gradle.structure.model.PsPath

class SingleModuleIssuesRenderer(context: PsContext) : DependencyViewIssuesRenderer(context) {
  private val issueRenderer = DependencyViewIssueRenderer(context, renderDescription = true)

  override fun render(issues: Collection<PsIssue>, scope: PsPath?): String = buildString {
    append("<html><body><ol>")

    for (issue in issues) {
      append("<li>")
      issueRenderer.renderIssue(this, issue, scope)
      append("</li>")
    }

    append("</ol></body></html>")
  }
}
