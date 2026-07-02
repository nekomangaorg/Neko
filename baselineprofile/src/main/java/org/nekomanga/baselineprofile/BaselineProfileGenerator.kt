package org.nekomanga.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "org.nekomanga.neko",
        includeInStartupProfile = true
    ) {
        // Go back to the home screen to ensure a clean cold start state
        pressHome()

        // Start the main activity
        startActivityAndWait()

        // Wait for the app to initialize, fetch local data, and display the first screen.
        // We sleep for 5 seconds to ensure typical startup paths are captured.
        Thread.sleep(5000)
    }
}

private fun UiDevice.waitAndClick(by: BySelector) {
    wait(Until.findObject(by), 60_000)?.click()
}

