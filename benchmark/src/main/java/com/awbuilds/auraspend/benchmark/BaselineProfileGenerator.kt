package com.awbuilds.auraspend.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline profile for AuraSpend.
 *
 * Run against a device/emulator (English or otherwise — navigation targets are
 * matched by contentDescription, which is not localized):
 *
 * ```
 * ./gradlew :benchmark:connectedFreeBenchmarkAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.awbuilds.auraspend.benchmark.BaselineProfileGenerator
 * ```
 *
 * The generated `baseline-prof.txt` is pulled into
 * `benchmark/build/outputs/connected_android_test_additional_output/…`;
 * copy it to `app/src/main/baseline-prof.txt` and commit.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = StartupBenchmark.PACKAGE) {
            startActivityAndWait()
            device.waitForIdle()

            // Warm the tab chrome and the Quick Add sheet — the hot paths.
            device.findObject(By.desc("Activity"))?.click()
            device.waitForIdle()
            device.findObject(By.desc("Home"))?.click()
            device.waitForIdle()
            device.findObject(By.desc("Add transaction"))?.click()
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()
        }
    }
}
