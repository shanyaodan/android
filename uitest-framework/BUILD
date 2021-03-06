load("//tools/base/bazel:bazel.bzl", "iml_module")
load("//tools/base/bazel:maven.bzl", "maven_repo")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.guiTestFramework",
    srcs = ["src"],
    iml_files = ["intellij.android.guiTestFramework.iml"],
    tags = ["no_test_windows"],
    test_class = "com.android.tools.idea.tests.gui.framework.GuiTestFrameworkTestSuite",
    test_data = [
        "//tools/idea:platform-build-info",
    ],
    test_srcs = ["testSrc"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    runtime_deps = [
        "//tools/idea/platform/tasks-platform-impl:intellij.platform.tasks.impl",
        "//tools/idea/json:intellij.json",
        "//tools/idea/platform/credential-store:intellij.platform.credentialStore",
        "//tools/idea/jvm/jvm-analysis-impl:intellij.jvm.analysis.impl",
        "//tools/idea/java/compiler/instrumentation-util-8:intellij.java.compiler.instrumentationUtil.java8",
        "//tools/idea/plugins/maven/error-prone-compiler:intellij.maven.errorProne.compiler",
        "//tools/idea/plugins/IntelliLang/intellilang-jps-plugin:intellij.java.langInjection.jps",
        "//tools/adt/idea/adt-branding:intellij.android.adt.branding",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/base/common:studio.android.sdktools.common[module, test]",
        "//tools/adt/idea/android:intellij.android.core[module]",
        "//tools/idea/platform/platform-api:intellij.platform.ide[module, test]",
        "//tools/idea/platform/platform-impl:intellij.platform.ide.impl[module, test]",
        "//tools/swing-testing/fest-swing[module, test]",
        "//tools/idea/.idea/libraries:jsr305[test]",
        "//tools/idea/.idea/libraries:truth[test]",
        "//tools/idea/platform/testFramework:intellij.platform.testFramework[module, test]",
        "//tools/idea/plugins/gradle/java:intellij.gradle.java[module, test]",
        "//tools/idea/java/openapi:intellij.java[module, test]",
        "//tools/idea/platform/bootstrap:intellij.platform.bootstrap[module, test]",
        "//tools/idea:intellij.platform.debugger.impl_and_others[module, test]",
        "//tools/idea/xml/dom-openapi:intellij.xml.dom[module, test]",
        "//tools/adt/idea/android/lib:spantable[test]",
        "//tools/adt/idea/adt-ui:intellij.android.adt.ui[module, test]",
        "//tools/adt/idea/property-editor:intellij.android.property-editor[module, test]",
        "//tools/adt/idea/designer:intellij.android.designer[module, test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/adt/idea/android-test-framework:intellij.android.testFramework[module, test]",
        "//tools/studio/google/cloud/testing/test-recorder[module, test]",
        "//tools/base/flags:studio.android.sdktools.flags[module, test]",
        "//tools/adt/idea/android-common:intellij.android.common[module, test]",
        "//tools/base/perf-logger:studio.perf-logger[module, test]",
        "//tools/adt/idea/project-system:intellij.android.projectSystem[module, test]",
        "//tools/idea/.idea/libraries:kotlin-stdlib-jdk8",
        "//tools/adt/idea/android-layout-inspector[module, test]",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/adt/idea/profilers-android:intellij.android.profilersAndroid[module, test]",
        "//tools/idea/platform/testFramework/bootstrap:intellij.tools.testsBootstrap[module, test]",
        "//tools/base/layoutinspector:studio.android.sdktools.layoutinspector[module, test]",
        "//tools/adt/idea/naveditor:intellij.android.naveditor[module, test]",
        "//tools/adt/idea/artwork:intellij.android.artwork[module, test]",
    ],
)

# Dependencies of UI tests not covered by //tools/adt/idea/android:test_deps, //prebuilts/tools/common/offline-m2, or out/studio/repo. This
# allows us to run UI tests on a release bundle without a source checkout.
maven_repo(
    name = "uitest_deps",
    artifacts = [
        "//prebuilts/tools/common/m2/repository/android/arch/lifecycle/runtime/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/constraintlayout/constraintlayout/1.1.2:aar",
        "//prebuilts/tools/common/m2/repository/androidx/constraintlayout/constraintlayout-solver/1.1.2:jar",
        "//prebuilts/tools/common/m2/repository/androidx/leanback/leanback/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/legacy/legacy-support-v4/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/media/media/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/percentlayout/percentlayout/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/recyclerview/recyclerview/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/wear/wear/1.0.0:aar",
        "//prebuilts/tools/common/m2/repository/androidx/test/espresso/espresso-core/3.1.0-alpha3:aar",
        "//prebuilts/tools/common/m2/repository/androidx/test/espresso/espresso-idling-resource/3.1.0-alpha3:aar",
        "//prebuilts/tools/common/m2/repository/androidx/test/monitor/1.1.0-alpha3:aar",
        "//prebuilts/tools/common/m2/repository/androidx/test/runner/1.1.0-alpha3:aar",
        "//prebuilts/tools/common/m2/repository/com/android/support/mediarouter-v7/27.1.1:aar",
        "//prebuilts/tools/common/m2/repository/com/google/android/material/material/1.0.0:aar",
    ],
    visibility = ["//visibility:public"],
)
