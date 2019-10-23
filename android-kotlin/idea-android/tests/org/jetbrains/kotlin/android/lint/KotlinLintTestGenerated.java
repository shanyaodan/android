/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.lint;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea-android/testData/android/lint")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class KotlinLintTestGenerated extends AbstractKotlinLintTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testDisabled() {}
    /* TODO(b/117954721): The Kotlin Lint tests in android-kotlin are flaky due to spurious errors from the Kotlin compiler.
    @TestMetadata("alarm.kt")
    public void testAlarm() throws Exception {
        runTest("idea-android/testData/android/lint/alarm.kt");
    }

    @TestMetadata("apiCheck.kt")
    public void testApiCheck() throws Exception {
        runTest("idea-android/testData/android/lint/apiCheck.kt");
    }

    @TestMetadata("callSuper.kt")
    public void testCallSuper() throws Exception {
        runTest("idea-android/testData/android/lint/callSuper.kt");
    }

    @TestMetadata("closeCursor.kt")
    public void testCloseCursor() throws Exception {
        runTest("idea-android/testData/android/lint/closeCursor.kt");
    }

    @TestMetadata("commitFragment.kt")
    public void testCommitFragment() throws Exception {
        runTest("idea-android/testData/android/lint/commitFragment.kt");
    }

    @TestMetadata("findViewById.kt")
    public void testFindViewById() throws Exception {
        runTest("idea-android/testData/android/lint/findViewById.kt");
    }

    @TestMetadata("javaPerformance.kt")
    public void testJavaPerformance() throws Exception {
        runTest("idea-android/testData/android/lint/javaPerformance.kt");
    }

    @TestMetadata("javaScriptInterface.kt")
    public void testJavaScriptInterface() throws Exception {
        runTest("idea-android/testData/android/lint/javaScriptInterface.kt");
    }

    @TestMetadata("layoutInflation.kt")
    public void testLayoutInflation() throws Exception {
        runTest("idea-android/testData/android/lint/layoutInflation.kt");
    }

    @TestMetadata("log.kt")
    public void testLog() throws Exception {
        runTest("idea-android/testData/android/lint/log.kt");
    }

    @TestMetadata("noInternationalSms.kt")
    public void testNoInternationalSms() throws Exception {
        runTest("idea-android/testData/android/lint/noInternationalSms.kt");
    }

    @TestMetadata("overrideConcrete.kt")
    public void testOverrideConcrete() throws Exception {
        runTest("idea-android/testData/android/lint/overrideConcrete.kt");
    }

    @TestMetadata("parcel.kt")
    public void testParcel() throws Exception {
        runTest("idea-android/testData/android/lint/parcel.kt");
    }

    @TestMetadata("sdCardTest.kt")
    public void testSdCardTest() throws Exception {
        runTest("idea-android/testData/android/lint/sdCardTest.kt");
    }

    @TestMetadata("setJavaScriptEnabled.kt")
    public void testSetJavaScriptEnabled() throws Exception {
        runTest("idea-android/testData/android/lint/setJavaScriptEnabled.kt");
    }

    @TestMetadata("sharedPrefs.kt")
    public void testSharedPrefs() throws Exception {
        runTest("idea-android/testData/android/lint/sharedPrefs.kt");
    }

    @TestMetadata("showDiagnosticsWhenFileIsRed.kt")
    public void testShowDiagnosticsWhenFileIsRed() throws Exception {
        runTest("idea-android/testData/android/lint/showDiagnosticsWhenFileIsRed.kt");
    }

    @TestMetadata("sqlite.kt")
    public void testSqlite() throws Exception {
        runTest("idea-android/testData/android/lint/sqlite.kt");
    }

    @TestMetadata("supportAnnotation.kt")
    public void testSupportAnnotation() throws Exception {
        runTest("idea-android/testData/android/lint/supportAnnotation.kt");
    }

    @TestMetadata("systemServices.kt")
    public void testSystemServices() throws Exception {
        runTest("idea-android/testData/android/lint/systemServices.kt");
    }

    @TestMetadata("toast.kt")
    public void testToast() throws Exception {
        runTest("idea-android/testData/android/lint/toast.kt");
    }

    @TestMetadata("valueOf.kt")
    public void testValueOf() throws Exception {
        runTest("idea-android/testData/android/lint/valueOf.kt");
    }

    @TestMetadata("velocityTrackerRecycle.kt")
    public void testVelocityTrackerRecycle() throws Exception {
        runTest("idea-android/testData/android/lint/velocityTrackerRecycle.kt");
    }

    @TestMetadata("viewConstructor.kt")
    public void testViewConstructor() throws Exception {
        runTest("idea-android/testData/android/lint/viewConstructor.kt");
    }

    @TestMetadata("viewHolder.kt")
    public void testViewHolder() throws Exception {
        runTest("idea-android/testData/android/lint/viewHolder.kt");
    }

    @TestMetadata("wrongAnnotation.kt")
    public void testWrongAnnotation() throws Exception {
        runTest("idea-android/testData/android/lint/wrongAnnotation.kt");
    }

    @TestMetadata("wrongImport.kt")
    public void testWrongImport() throws Exception {
        runTest("idea-android/testData/android/lint/wrongImport.kt");
    }

    @TestMetadata("wrongViewCall.kt")
    public void testWrongViewCall() throws Exception {
        runTest("idea-android/testData/android/lint/wrongViewCall.kt");
    }
    TODO(b/117954721): The Kotlin Lint tests in android-kotlin are flaky due to spurious errors from the Kotlin compiler. */
}
