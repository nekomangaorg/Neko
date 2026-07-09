package org.nekomanga.baselineprofile

import android.content.Context
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
        val targetPackage = "org.nekomanga"
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Disable onboarding and notification permission dialogs before starting the app.
        // This ensures the generator lands directly on the main library screen.
        val sharedPreferences = targetContext.getSharedPreferences(
            "${targetPackage}_preferences",
            Context.MODE_PRIVATE,
        )
        sharedPreferences.edit()
            .putBoolean("__APP_STATE_onboarding_complete", true)
            .putBoolean("has_shown_notification_permission", true)
            .apply()

        rule.collect(
            packageName = "org.nekomanga.neko",
            includeInStartupProfile = true,
            maxIterations = 1,
            stableIterations = 1

        ) {
            // Go back to the home screen to ensure a clean cold start state
            pressHome()

            // Start the main activity
            startActivityAndWait()

            val appOpened = device.wait(Until.hasObject(By.pkg(targetPackage)), 15_000)
            if (!appOpened) {
                throw RuntimeException("App failed to launch for baseline profile generation")
            }

            // Wait for the package to appear on the screen (timeout of 10 seconds)
            device.wait(Until.hasObject(By.pkg(targetPackage)), 10_000)

            // Wait for the main Library screen tab text to render (timeout of 10 seconds)
            device.wait(Until.hasObject(By.text("Library")), 10_000)

            // Click the Feed tab to record feed compilation paths
            device.waitAndClick(By.text("Feed"))
            device.waitForIdle()

            // Navigate back to the Library tab
            device.waitAndClick(By.text("Library"))
            device.waitForIdle()
        }
    }
}

private fun UiDevice.waitAndClick(by: BySelector) {
    wait(Until.findObject(by), 60_000)?.click()
}
