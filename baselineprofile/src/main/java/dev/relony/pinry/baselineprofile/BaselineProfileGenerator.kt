package dev.relony.pinry.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of startup, feed scrolling and opening a pin into the app's Baseline
 * Profile (`app/src/release/generated/baselineProfiles/`). Needs a device or emulator and a Pinry
 * account; the credentials are passed on the command line and never stored:
 *
 * ```
 * ./gradlew :app:generateBaselineProfile \
 *   -Pandroid.testInstrumentationRunnerArguments.pinryServer=pinry.example.com \
 *   -Pandroid.testInstrumentationRunnerArguments.pinryUser=… \
 *   -Pandroid.testInstrumentationRunnerArguments.pinryPassword=…
 * ```
 *
 * The benchmarks run the same way with `:baselineprofile:connectedBenchmarkReleaseAndroidTest`, one
 * class at a time via `-Pandroid.testInstrumentationRunnerArguments.class=…` (plus
 * `…androidx.benchmark.suppressErrors=EMULATOR` on an emulator). Note that connected tests uninstall
 * the app afterwards.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Before fun login() = ensureLoggedIn()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForFeed(device)
        scrollFeed(device)
        openPinAndBack(device)
    }
}
