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
package com.android.tools.idea.stats

import com.android.tools.analytics.CommonMetricsData
import com.android.tools.analytics.UsageTracker
import com.android.tools.idea.diagnostics.AndroidStudioSystemHealthMonitor
import com.android.tools.idea.diagnostics.report.MemoryReportReason
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.LowMemoryWatcher
import java.util.concurrent.TimeUnit

internal class LowMemoryReporter : Disposable {
  private var lowMemoryWatcherAll: LowMemoryWatcher? = null
  private var lowMemoryWatcherAfterGc: LowMemoryWatcher? = null

  private val limiter = EventsLimiter(3, TimeUnit.MINUTES.toMillis(1), singleShotOnly = true)

  init {
    lowMemoryWatcherAll = LowMemoryWatcher.register {
      // According to https://docs.oracle.com/javase/7/docs/api/java/lang/management/MemoryNotificationInfo.html#MEMORY_THRESHOLD_EXCEEDED,
      // the notification is emitted once when the threshold is exceeded, and is not emitted again until the VM goes below the threshold.
      // So we don't have to explicitly rate limit our reports.
      UsageTracker.log(AndroidStudioEvent.newBuilder()
                         .setKind(AndroidStudioEvent.EventKind.STUDIO_LOW_MEMORY_EVENT)
                         .setJavaProcessStats(CommonMetricsData.javaProcessStats))
      if (limiter.tryAcquire()) {
        AndroidStudioSystemHealthMonitor.getInstance()?.lowMemoryDetected(MemoryReportReason.FrequentLowMemoryNotification)
      }
    }
    lowMemoryWatcherAfterGc = LowMemoryWatcher.register({
      val wasDisabled = limiter.disable()
      if (!wasDisabled) {
        AndroidStudioSystemHealthMonitor.getInstance()?.lowMemoryDetected(MemoryReportReason.LowMemory)
      }
    }, LowMemoryWatcher.LowMemoryWatcherType.ONLY_AFTER_GC)
  }

  override fun dispose() {
    lowMemoryWatcherAll?.stop()
    lowMemoryWatcherAll = null
    lowMemoryWatcherAfterGc?.stop()
    lowMemoryWatcherAfterGc = null
  }
}