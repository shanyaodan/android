cc_library(
    name = "jnibleakhelper",
    srcs = [
        "jniBleakHelper.cpp",
        "jniBleakHelper.h",
    ],
    copts = [
        "-fPIC",
        "-shared",
    ] + select({
        "//tools/base/bazel:darwin": [
            "-Iprebuilts/studio/jdk/mac/Contents/Home/include",
        ],
        "//conditions:default": [
            "-Iprebuilts/studio/jdk/linux/include",
            "-Iprebuilts/studio/jdk/linux/include/linux",
        ],
    }),
    tags = ["no_windows"],
    visibility = ["//visibility:public"],
    deps = [
        "//prebuilts/studio/jdk:jni_headers",
    ],
)

java_binary(
    name = "ObjectSizeInstrumentationAgent",
    srcs = [
        "ObjectSizeInstrumentationAgent.java",
    ],
    create_executable = False,
    deploy_manifest_lines = [
        "Premain-class: com.android.tools.idea.tests.gui.framework.heapassertions.bleak.",
        " agents.ObjectSizeInstrumentationAgent",
    ],
    visibility = ["//visibility:public"],
)
