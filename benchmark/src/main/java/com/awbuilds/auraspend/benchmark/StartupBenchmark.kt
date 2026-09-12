package com.awbuilds.auraspend.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold-start budget for AuraSpend.
 *
 * Run on a release-like build:
 * `./gradlew :benchmark:connectedFreeBenchmarkAndroidTest`
 *
 * Target (Pixel 6a class): time-to-initial-display < 700 ms.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }

    companion object {
        const val PACKAGE = "com.awbuilds.auraspend"
    }
}
