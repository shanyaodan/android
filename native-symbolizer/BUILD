load("//tools/base/bazel:bazel.bzl", "iml_module")

# managed by go/iml_to_build
iml_module(
    name = "native-symbolizer",
    # do not sort: must match IML order
    srcs = ["src"],
    iml_files = ["native-symbolizer.iml"],
    tags = ["no_test_windows"],
    test_class = "com.android.tools.nativeSymbolizer.NativeSymbolizerTestSuite",
    test_coverage = True,
    test_data = glob(["testData/**"]) + [
        "//prebuilts/tools:lldb",
        "//tools/idea:platform-build-info",
    ],
    test_resources = ["testData"],
    test_srcs = ["testSrc"],
    test_timeout = "short",
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
        "//tools/idea/.idea/libraries:kotlin-stdlib-jdk8",
        "//tools/idea/platform/core-api:intellij.platform.core[module]",
        "//tools/adt/idea/android:intellij.android.core[module]",
        "//tools/adt/idea/android-common:intellij.android.common[module]",
        "//tools/idea/platform/projectModel-api:intellij.platform.projectModel[module]",
        "//tools/idea/platform/lang-api:intellij.platform.lang[module]",
        "//tools/vendor/intellij/cidr/cidr-debugger:intellij.cidr.debugger[module]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/idea/platform/core-impl:intellij.platform.core.impl[module]",
    ],
)
