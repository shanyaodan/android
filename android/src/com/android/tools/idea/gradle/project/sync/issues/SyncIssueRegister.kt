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
package com.android.tools.idea.gradle.project.sync.issues

import com.android.builder.model.SyncIssue
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val LOGGER = Logger.getInstance(SyncIssueRegister::class.java)

/**
 * A project based component that stores a map from modules to sync issues. These are registered during sync (module setup) and are reported
 * shortly afterward. The register is cleared at the start of each sync.
 *
 * [SyncIssue]s should not be read until this object has been sealed (it is sealed at the end of sync) as there still may be additional
 * [SyncIssue]s that have not been reported, attempting to do so will log an error.
 *
 * Likewise once this object is sealed any attempt to register additional issues will also log an error. These errors will be converted
 * into exception at a later date.
 */
class SyncIssueRegister(val project: Project) : Sealable by BaseSealable() {
  private val lock = ReentrantLock()
  private val syncIssueMap: MutableMap<String, MutableList<SyncIssue>> = mutableMapOf()

  fun register(module: Module, syncIssues: Collection<SyncIssue>) {
    lock.withLock {
      if (checkSeal()) LOGGER.error("Attempted to add more sync issues when the SyncIssueRegister was sealed!")
      syncIssueMap.computeIfAbsent(module.name) { ArrayList() }.addAll(syncIssues)
    }
  }

  fun get(): Map<Module, List<SyncIssue>> {
    return lock.withLock {
      if (!checkSeal()) LOGGER.error("Attempted to read sync issues before the SyncIssuesRegister was sealed!")
      val moduleManager = ModuleManager.getInstance(project)
      val modules = syncIssueMap.keys.mapNotNull { moduleManager.findModuleByName(it) }
      modules.map { it to syncIssueMap[it.name].orEmpty() }.toMap()
    }
  }

  fun unsealAndClear() {
    lock.withLock {
      unseal()
      syncIssueMap.clear()
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SyncIssueRegister {
      return ServiceManager.getService(project, SyncIssueRegister::class.java)
    }
  }
}
