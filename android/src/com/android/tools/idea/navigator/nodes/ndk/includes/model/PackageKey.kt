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
package com.android.tools.idea.navigator.nodes.ndk.includes.model

import java.io.File

/**
 * Package grouping key. This key groups packages by package type, package name, and base folder for the package family.
 */
data class PackageKey(
  // The packaging kind. For example, NDK component.
  val packageType: PackageType,
  // The name of the package
  val simplePackageName: String,
  // The root folder of the packaging. For example Android NDK root folder
  val packagingFamilyBaseFolder: File)
