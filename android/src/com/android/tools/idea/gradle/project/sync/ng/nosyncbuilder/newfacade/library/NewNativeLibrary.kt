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
package com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.newfacade.library

import com.android.ide.common.gradle.model.IdeNativeLibrary
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.interfaces.library.NativeLibrary
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.misc.PathConverter
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.proto.LibraryProto
import java.io.File

data class NewNativeLibrary(
  override val abi: String,
  override val toolchainName: String,
  override val cCompilerFlags: Collection<String>,
  override val cppCompilerFlags: Collection<String>,
  override val debuggableLibraryFolders: Collection<File>,
  override val artifactAddress: String? = null // not supported for native libraries
) : NativeLibrary {
  constructor(library: IdeNativeLibrary) : this(
    library.abi,
    library.toolchainName,
    library.cCompilerFlags,
    library.cppCompilerFlags,
    library.debuggableLibraryFolders
  )

  constructor(proto: LibraryProto.NativeLibrary, converter: PathConverter) : this(
    proto.abi,
    proto.toolchainName,
    proto.cCompilerFlagsList,
    proto.cppCompilerFlagsList,
    proto.debuggableLibraryFoldersList.map { converter.fileFromProto(it) }
  )
}
