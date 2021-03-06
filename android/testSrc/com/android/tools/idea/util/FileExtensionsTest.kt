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
package com.android.tools.idea.util

import com.android.tools.idea.apk.viewer.ApkFileSystem
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import org.jetbrains.android.AndroidTestCase
import java.io.File

class FileExtensionsTest : AndroidTestCase() {
  fun testRegularFile() {
    val ioFile = File(getTestDataPath()).resolve("AndroidManifest.xml").canonicalFile
    assertTrue(ioFile.exists())
    val vfsFile = ioFile.toVirtualFile(refresh = true)!!

    checkPathStringFromVirtualFile(vfsFile)

    assertEquals(ioFile, vfsFile.toIoFile())
    assertEquals(vfsFile, ioFile.toVirtualFile())
    assertEquals(ioFile, vfsFile.toPathString().toFile())
  }

  fun testApk() {
    val ioFile = File(getTestDataPath()).resolve("design_aar").resolve("res.apk").canonicalFile
    assertTrue(ioFile.exists())
    val entryPath = ioFile.absolutePath + ApkFileSystem.APK_SEPARATOR + "res/drawable-mdpi-v4/design_ic_visibility.png"
    val apkFsUrl = ApkFileSystem.PROTOCOL + "://" + entryPath

    val vfsFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(apkFsUrl)!!
    checkPathStringFromVirtualFile(vfsFile)

    val pathString = vfsFile.toPathString()
    assertThat(pathString.filesystemUri.scheme).isEqualTo(ApkFileSystem.PROTOCOL)
    assertThat(pathString.rawPath).isEqualTo(entryPath)
  }

  fun testTemp() {
    val vfsFile = runWriteAction {
      TempFileSystem.getInstance()
        .findFileByPath("/")!!
        .createChildData(this, FileExtensionsTest::class.qualifiedName!!)
        .apply { setBinaryContent("hello".toByteArray()) }
    }

    checkPathStringFromVirtualFile(vfsFile)
  }

  private fun checkPathStringFromVirtualFile(vfsFile: VirtualFile) {
    assertTrue(vfsFile.exists())

    // VFS uses three slashes in its URLs and won't recognize URLs like `file:/foo` or `temp:/foo`. Make sure we're compatible with that.
    val threeSlashes = ":///"

    assertTrue(vfsFile.url.contains(threeSlashes))

    val pathString = vfsFile.toPathString()
    assertThat(pathString.filesystemUri.path).isEqualTo(File.separator)

    assertEquals(vfsFile, pathString.toVirtualFile())
    assertEquals(vfsFile, VirtualFileManager.getInstance().refreshAndFindFileByUrl(pathString.toString()))
  }
}
