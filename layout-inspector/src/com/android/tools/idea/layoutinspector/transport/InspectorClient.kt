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
package com.android.tools.idea.layoutinspector.transport

import com.android.tools.layoutinspector.proto.LayoutInspectorProto.LayoutInspectorCommand
import com.android.tools.layoutinspector.proto.LayoutInspectorProto.LayoutInspectorEvent
import com.android.tools.profiler.proto.Common
import com.android.tools.profiler.proto.Common.Event.EventGroupIds
import com.google.common.annotations.VisibleForTesting

/**
 * Client for communicating with the agent.
 */
interface InspectorClient {
  /**
   * Register a handler for a specific groupId.
   */
  fun register(groupId: EventGroupIds, callback: (LayoutInspectorEvent) -> Unit)

  /**
   * Find all processes that the inspector can attach to.
   */
  fun loadProcesses(): Map<Common.Stream, List<Common.Process>>

  /**
   * Attach to a specific process.
   */
  fun attach(stream: Common.Stream, process: Common.Process)

  /**
   * Send a command to the agent.
   */
  fun execute(command: LayoutInspectorCommand)

  /**
   * Fetch the payload from a given payload [id].
   */
  fun getPayload(id: Int): ByteArray

  companion object {

    val instance: InspectorClient
      get() = currentInstance ?: createDefaultInstance()

    private fun createDefaultInstance(): InspectorClient {
      val client = DefaultInspectorClient
      currentInstance = client
      return client
    }

    @VisibleForTesting
    internal var currentInstance: InspectorClient? = null
  }
}
