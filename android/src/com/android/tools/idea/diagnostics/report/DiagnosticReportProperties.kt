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
package com.android.tools.idea.diagnostics.report

import com.android.tools.analytics.AnalyticsSettings
import com.android.tools.analytics.UsageTracker
import com.intellij.ide.plugins.PluginManagerCore
import java.lang.management.ManagementFactory
import java.util.Arrays

data class DiagnosticReportProperties(
  val uptime: Long = ManagementFactory.getRuntimeMXBean().uptime,
  val reportTime: Long = AnalyticsSettings.dateProvider.now().time,
  val sessionId: String? = UsageTracker.sessionId,
  val studioVersion: String? = UsageTracker.version,
  val kotlinVersion: String? = computeKotlinVersion()
) {
  fun asProductDataMap(): Map<String, String> {
    val map = mutableMapOf("reportTime" to reportTime.toString())
    sessionId?.let { map["sessionId"] = it }
    kotlinVersion?.let { map["kotlinVersion"] = it }
    return map
  }

  companion object {
    private fun computeKotlinVersion(): String? {
      return try {
        Arrays.stream(PluginManagerCore.getPlugins())
          .filter { d ->
            "org.jetbrains.kotlin" == d.pluginId?.idString && d.isEnabled
          }
          .findFirst()
          .map { d -> d.version }
          .orElse(null)
      }
      catch (ignored: Throwable) {
        null
      }
    }
  }
}