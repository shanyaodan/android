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
package com.android.tools.idea.res

import com.android.tools.idea.resources.aar.FrameworkResourceRepository
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.ide.PooledThreadExecutor
import java.io.File

/**
 * Application service for caching and reusing instances of [FrameworkResourceRepository].
 */
class FrameworkResourceRepositoryManager {
  companion object {
    @JvmStatic fun getInstance() = ServiceManager.getService(FrameworkResourceRepositoryManager::class.java)!!
  }

  private data class Key(val resFolderOrJar: File, val needLocales: Boolean)

  private val cache: LoadingCache<Key, FrameworkResourceRepository> = CacheBuilder.newBuilder()
    .softValues()
    .build(CacheLoader.from { key ->
      FrameworkResourceRepository.create(key!!.resFolderOrJar.toPath(), key.needLocales, true, PooledThreadExecutor.INSTANCE)
    })

  /**
   * Returns a [FrameworkResourceRepository] for the given `resFolder`. The `needLocales` argument is used to indicate if locale-specific
   * information is needed, which makes computing the repository much slower. Even if `needLocales` is false, a repository with locale
   * information may be returned if it has been computed earlier and is available.
   */
  fun getFrameworkResources(resFolderOrJar: File, needLocales: Boolean): FrameworkResourceRepository {
    return if (needLocales) {
      cache.get(Key(resFolderOrJar, true)).also { cache.invalidate(Key(resFolderOrJar, false)) }
    } else {
      cache.getIfPresent(Key(resFolderOrJar, true)) ?: cache.get(Key(resFolderOrJar, false))
    }
  }
}
