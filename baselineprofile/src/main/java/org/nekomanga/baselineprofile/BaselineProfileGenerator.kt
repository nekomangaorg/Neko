package org.nekomanga.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
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
    fun generate() {
        val targetPackage = InstrumentationRegistry.getInstrumentation().targetContext.packageName

        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = true
        ) {
            // Go back to the home screen to ensure a clean cold start state
            pressHome()

            // Start the main activity
            startActivityAndWait()

            // Wait for the package to appear on the screen (timeout of 10 seconds)
            device.wait(Until.hasObject(By.pkg(targetPackage)), 10_000)

            // Wait for the main Library screen tab text to render (timeout of 10 seconds)
            device.wait(Until.hasObject(By.text("Library")), 10_000)
        }
    }
}

private fun UiDevice.waitAndClick(by: BySelector) {
    wait(Until.findObject(by), 60_000)?.click()
}
