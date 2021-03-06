#ifndef THREAD_PAUSER_H
#define THREAD_PAUSER_H
#include <jvmti.h>

extern "C" {
  JNIEXPORT void JNICALL Java_com_android_tools_idea_tests_gui_framework_heapassertions_bleak_JniBleakHelper_pauseThreads0(JNIEnv *env);
  JNIEXPORT void JNICALL Java_com_android_tools_idea_tests_gui_framework_heapassertions_bleak_JniBleakHelper_resumeThreads0(JNIEnv *env);
  JNIEXPORT jobjectArray JNICALL Java_com_android_tools_idea_tests_gui_framework_heapassertions_bleak_JniBleakHelper_gcRoots(JNIEnv *env);
  JNIEXPORT jobjectArray JNICALL Java_com_android_tools_idea_tests_gui_framework_heapassertions_bleak_JniBleakHelper_allLoadedClasses0(JNIEnv *env);
}

#endif
