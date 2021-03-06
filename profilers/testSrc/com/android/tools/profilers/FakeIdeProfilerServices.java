/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.profilers;

import com.android.tools.profiler.proto.Cpu;
import com.android.tools.profilers.analytics.FeatureTracker;
import com.android.tools.profilers.cpu.FakeTracePreProcessor;
import com.android.tools.profilers.cpu.ProfilingConfiguration;
import com.android.tools.profilers.cpu.TracePreProcessor;
import com.android.tools.profilers.stacktrace.CodeNavigator;
import com.android.tools.profilers.stacktrace.FakeCodeNavigator;
import com.android.tools.profilers.stacktrace.NativeFrameSymbolizer;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FakeIdeProfilerServices implements IdeProfilerServices {

  public static final String FAKE_ART_SAMPLED_NAME = "Sampled";

  public static final String FAKE_ART_INSTRUMENTED_NAME = "Instrumented";

  public static final String FAKE_SIMPLEPERF_NAME = "Simpleperf";

  public static final String FAKE_ATRACE_NAME = "Atrace";

  private final FeatureTracker myFakeFeatureTracker = new FakeFeatureTracker();
  private final NativeFrameSymbolizer myFakeSymbolizer = (abi, nativeFrame) -> nativeFrame;
  private final CodeNavigator myFakeNavigationService = new FakeCodeNavigator(myFakeFeatureTracker);
  private final TracePreProcessor myFakeTracePreProcessor = new FakeTracePreProcessor();

  /**
   * Callback to be run after the executor calls its execute() method.
   */
  @Nullable
  Runnable myOnExecute;

  /**
   * The pool executor runs code in a separate thread. Sometimes is useful to check the state of the profilers
   * just before calling pool executor's execute method (e.g. verifying Stage's transient status before making a gRPC call).
   */
  @Nullable
  Runnable myPrePoolExecute;

  /**
   * Can toggle for tests via {@link #enableAtrace(boolean)}, but each test starts with this defaulted to false.
   */
  private boolean myAtraceEnabled = false;

  /**
   * Can toggle for tests via {@link #enablePerfetto(boolean)}, but each test starts with this defaulted to false. Enabling this flag
   * assumes that {@link #myAtraceEnabled} is true.
   */
  private boolean myPerfettoEnabled = false;

  /**
   * Toggle for including an energy profiler in our profiler view.
   */
  private boolean myEnergyProfilerEnabled = false;

  /**
   * Can toggle for tests via {@link #enableExportTrace(boolean)}, but each test starts with this defaulted to false.
   */
  private boolean myExportCpuTraceEnabled = false;

  /**
   * Toggle for faking fragments UI support in tests.
   */
  private boolean myFragmentsEnabled = true;

  /**
   * Can toggle for tests via {@link #enableImportTrace(boolean)}, but each test starts with this defaulted to false.
   */
  private boolean myImportCpuTraceEnabled = false;

  /**
   * Can toggle for tests via {@link #enableSimpleperfHost(boolean)}, but each test starts with this defaulted to false.
   */
  private boolean mySimpleperfHostEnabled = false;

  /**
   * JNI references alloc/dealloc events are tracked and shown.
   */
  private boolean myIsJniReferenceTrackingEnabled = false;

  /**
   * Toggle for faking live allocation tracking support in tests.
   */
  private boolean myLiveTrackingEnabled = false;

  /**
   * Toggle for faking memory snapshot support in tests.
   */
  private boolean myMemorySnapshotEnabled = true;

  /**
   * Whether a native CPU profiling configuration is preferred over a Java one.
   */
  private boolean myNativeProfilingConfigurationPreferred = false;

  /**
   * Whether long trace files should be parsed.
   */
  private boolean myShouldParseLongTraces = false;

  /**
   * Toggle for faking session import support in tests.
   */
  private boolean mySessionsImportEnabled = true;

  /**
   * Can toggle for tests via {@link #enableStartupCpuProfiling(boolean)}, but each test starts with this defaulted to false.
   */
  private boolean myStartupCpuProfilingEnabled = false;

  /**
   * Can toggle for tests via {@link #enableCpuApiTracing(boolean)}, but each test starts with this defaulted to false.
   */
  private boolean myIsCpuApiTracingEnabled = false;

  /**
   * Whether the new pipeline is used or the old one for devices / processes / sessions.
   */
  private boolean myEventsPipelineEnabled = false;

  /**
   * Toggle for faking {@link FeatureConfig#isCpuNewRecordingWorkflowEnabled()} in tests.
   */
  private boolean myCpuNewRecordingWorkflowEnabled = false;

  /**
   * Toggle for live allocation sampling mode.
   */
  private boolean myLiveAllocationsSamplingEnabled = true;

  /**
   * List of custom CPU profiling configurations.
   */
  private final List<ProfilingConfiguration> myCustomProfilingConfigurations = new ArrayList<>();

  @NotNull private final ProfilerPreferences myPersistentPreferences;
  @NotNull private final ProfilerPreferences myTemporaryPreferences;

  /**
   * When {@link #openListBoxChooserDialog} is called this index is used to return a specific element in the set of options.
   * If this index is out of bounds, null is returned.
   */
  private int myListBoxOptionsIndex;
  /**
   * Fake application id to be used by test.
   */
  private String myApplicationId = "";

  @Nullable private Notification myNotification;

  public FakeIdeProfilerServices() {
    myPersistentPreferences = new FakeProfilerPreferences();
    myTemporaryPreferences = new FakeProfilerPreferences();
  }

  @NotNull
  @Override
  public Executor getMainExecutor() {
    return (runnable) -> {
      runnable.run();
      if (myOnExecute != null) {
        myOnExecute.run();
      }
    };
  }

  @NotNull
  @Override
  public Executor getPoolExecutor() {
    return (runnable) -> {
      if (myPrePoolExecute != null) {
        myPrePoolExecute.run();
      }
      runnable.run();
    };
  }

  @Override
  public void saveFile(@NotNull File file, @NotNull Consumer<FileOutputStream> fileOutputStreamConsumer, @Nullable Runnable postRunnable) {
  }

  @NotNull
  @Override
  public NativeFrameSymbolizer getNativeFrameSymbolizer() {
    return myFakeSymbolizer;
  }

  @NotNull
  @Override
  public CodeNavigator getCodeNavigator() {
    return myFakeNavigationService;
  }

  @NotNull
  @Override
  public FeatureTracker getFeatureTracker() {
    return myFakeFeatureTracker;
  }

  @Override
  public void enableAdvancedProfiling() {
    // No-op.
  }

  @NotNull
  @Override
  public String getApplicationId() {
    return myApplicationId;
  }

  public void setApplicationId(@NotNull String name) {
    myApplicationId = name;
  }

  @NotNull
  @Override
  public FeatureConfig getFeatureConfig() {
    return new FeatureConfig() {
      @Override
      public boolean isAtraceEnabled() {
        return myAtraceEnabled;
      }

      @Override
      public boolean isCpuApiTracingEnabled() {
        return myIsCpuApiTracingEnabled;
      }

      @Override
      public boolean isCpuNewRecordingWorkflowEnabled() {
        return myCpuNewRecordingWorkflowEnabled;
      }

      @Override
      public boolean isEnergyProfilerEnabled() {
        return myEnergyProfilerEnabled;
      }

      @Override
      public boolean isExportCpuTraceEnabled() {
        return myExportCpuTraceEnabled;
      }

      @Override
      public boolean isFragmentsEnabled() {
        return myFragmentsEnabled;
      }

      @Override
      public boolean isImportCpuTraceEnabled() {
        return myImportCpuTraceEnabled;
      }

      @Override
      public boolean isJniReferenceTrackingEnabled() { return myIsJniReferenceTrackingEnabled; }

      @Override
      public boolean isLiveAllocationsEnabled() {
        return myLiveTrackingEnabled;
      }

      @Override
      public boolean isLiveAllocationsSamplingEnabled() {
        return myLiveAllocationsSamplingEnabled;
      }

      @Override
      public boolean isMemoryCaptureFilterEnabled() {
        return false;
      }

      @Override
      public boolean isMemorySnapshotEnabled() {
        return myMemorySnapshotEnabled;
      }

      @Override
      public boolean isPerfettoEnabled() { return myPerfettoEnabled; }

      @Override
      public boolean isPerformanceMonitoringEnabled() {
        return false;
      }

      @Override
      public boolean isSessionImportEnabled() {
        return mySessionsImportEnabled;
      }

      @Override
      public boolean isSimpleperfHostEnabled() {
        return mySimpleperfHostEnabled;
      }

      @Override
      public boolean isStartupCpuProfilingEnabled() {
        return myStartupCpuProfilingEnabled;
      }

      @Override
      public boolean isUnifiedPipelineEnabled() {
        return myEventsPipelineEnabled;
      }
    };
  }

  @NotNull
  @Override
  public ProfilerPreferences getTemporaryProfilerPreferences() {
    return myTemporaryPreferences;
  }

  @NotNull
  @Override
  public ProfilerPreferences getPersistentProfilerPreferences() {
    return myPersistentPreferences;
  }

  @Override
  public void openParseLargeTracesDialog(Runnable yesCallback, Runnable noCallback) {
    if (myShouldParseLongTraces) {
      yesCallback.run();
    }
    else {
      noCallback.run();
    }
  }

  @Override
  public <T> T openListBoxChooserDialog(@NotNull String title,
                                        @Nullable String message,
                                        @NotNull T[] options,
                                        @NotNull Function<T, String> listBoxPresentationAdapter) {
    if (myListBoxOptionsIndex >= 0 && myListBoxOptionsIndex < options.length) {
      return options[myListBoxOptionsIndex];
    }
    return null;
  }

  @NotNull
  @Override
  public TracePreProcessor getSimpleperfTracePreProcessor() {
    return myFakeTracePreProcessor;
  }

  /**
   * Sets the listbox options return element index. If this is set to an index out of bounds null is returned.
   */
  public void setListBoxOptionsIndex(int optionIndex) {
    myListBoxOptionsIndex = optionIndex;
  }

  public void setShouldParseLongTraces(boolean shouldParseLongTraces) {
    myShouldParseLongTraces = shouldParseLongTraces;
  }

  public void addCustomProfilingConfiguration(String name, Cpu.CpuTraceType type) {
    ProfilingConfiguration config =
      new ProfilingConfiguration(name, type, Cpu.CpuTraceMode.UNSPECIFIED_MODE);
    myCustomProfilingConfigurations.add(config);
  }

  @Override
  public List<ProfilingConfiguration> getUserCpuProfilerConfigs() {
    return myCustomProfilingConfigurations;
  }

  @Override
  public List<ProfilingConfiguration> getDefaultCpuProfilerConfigs() {
    ProfilingConfiguration artSampled = new ProfilingConfiguration(FAKE_ART_SAMPLED_NAME,
                                                                   Cpu.CpuTraceType.ART,
                                                                   Cpu.CpuTraceMode.SAMPLED);
    ProfilingConfiguration artInstrumented = new ProfilingConfiguration(FAKE_ART_INSTRUMENTED_NAME,
                                                                        Cpu.CpuTraceType.ART,
                                                                        Cpu.CpuTraceMode.INSTRUMENTED);
    ProfilingConfiguration simpleperf = new ProfilingConfiguration(FAKE_SIMPLEPERF_NAME,
                                                                   Cpu.CpuTraceType.SIMPLEPERF,
                                                                   Cpu.CpuTraceMode.SAMPLED);
    ProfilingConfiguration atrace = new ProfilingConfiguration(FAKE_ATRACE_NAME,
                                                               Cpu.CpuTraceType.ATRACE,
                                                               Cpu.CpuTraceMode.SAMPLED);
    return ImmutableList.of(artSampled, artInstrumented, simpleperf, atrace);
  }

  @Override
  public boolean isNativeProfilingConfigurationPreferred() {
    return myNativeProfilingConfigurationPreferred;
  }

  @Override
  public void showNotification(@NotNull Notification notification) {
    myNotification = notification;
  }

  @Override
  public void reportNoPiiException(@NotNull Throwable t) {
    t.printStackTrace();
  }

  @Nullable
  public Notification getNotification() {
    return myNotification;
  }

  public void setNativeProfilingConfigurationPreferred(boolean nativeProfilingConfigurationPreferred) {
    myNativeProfilingConfigurationPreferred = nativeProfilingConfigurationPreferred;
  }

  public void setOnExecute(@Nullable Runnable onExecute) {
    myOnExecute = onExecute;
  }

  public void setPrePoolExecutor(@Nullable Runnable prePoolExecute) {
    myPrePoolExecute = prePoolExecute;
  }

  public void enableAtrace(boolean enabled) {
    myAtraceEnabled = enabled;
  }

  public void enablePerfetto(boolean enabled) {
    myPerfettoEnabled = enabled;
  }

  public void enableEnergyProfiler(boolean enabled) {
    myEnergyProfilerEnabled = enabled;
  }

  public void enableFragments(boolean enabled) {
    myFragmentsEnabled = enabled;
  }

  public void enableJniReferenceTracking(boolean enabled) { myIsJniReferenceTrackingEnabled = enabled; }

  public void enableLiveAllocationTracking(boolean enabled) {
    myLiveTrackingEnabled = enabled;
  }

  public void enableStartupCpuProfiling(boolean enabled) {
    myStartupCpuProfilingEnabled = enabled;
  }

  public void enableCpuApiTracing(boolean enabled) {
    myIsCpuApiTracingEnabled = enabled;
  }

  public void enableExportTrace(boolean enabled) {
    myExportCpuTraceEnabled = enabled;
  }

  public void enableImportTrace(boolean enabled) {
    myImportCpuTraceEnabled = enabled;
  }

  public void enableSimpleperfHost(boolean enabled) {
    mySimpleperfHostEnabled = enabled;
  }

  public void enableEventsPipeline(boolean enabled) {
    myEventsPipelineEnabled = enabled;
  }

  public void enableCpuNewRecordingWorkflow(boolean enabled) {
    myCpuNewRecordingWorkflowEnabled = enabled;
  }

  public void enableLiveAllocationsSampling(boolean enabled) {
    myLiveAllocationsSamplingEnabled = enabled;
  }
}
