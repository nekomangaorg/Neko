package org.nekomanga.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val targetPackage = "org.nekomanga.neko"

        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = true,
            maxIterations = 1,
            stableIterations = 1
        ) {
            // Go back to the home screen to ensure a clean cold start state
            pressHome()

            // Start the main activity with the benchmark intent extra to bypass onboarding
            startActivityAndWait { intent ->
                intent.putExtra("is_benchmark", true)
            }

            // Wait for the package to appear on the screen (timeout of 15 seconds)
            val appOpened = device.wait(Until.hasObject(By.pkg(targetPackage)), 15_000)
            if (!appOpened) {
                throw RuntimeException("App failed to launch for baseline profile generation")
            }

            // Wait for the main screen (Library, Feed, or Browse) to render.
            // We wait up to 30 seconds to handle slow headless emulators in CI environments.
            val mainScreenLoaded = device.wait(
                Until.hasObject(By.text(Pattern.compile("Library|Feed|Browse"))),
                30_000
            )
            if (!mainScreenLoaded) {
                throw RuntimeException("Main screen (Library/Feed/Browse) failed to load on startup")
            }

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
    wait(Until.findObject(by), 15_000)?.click()
        ?: throw RuntimeException("Could not find element to click: $by")
}
