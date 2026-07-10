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
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val targetPackage = targetContext.packageName

        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = true,
            maxIterations = 1,
            stableIterations = 1
        ) {
            // Go back to the home screen to ensure a clean cold start state
            pressHome()

            // Start the main activity
            startActivityAndWait()

            // Wait for the package to appear on the screen (timeout of 15 seconds)
            val appOpened = device.wait(Until.hasObject(By.pkg(targetPackage)), 15_000)
            if (!appOpened) {
                throw RuntimeException("App failed to launch for baseline profile generation")
            }

            // Click through onboarding if it is showing (indicated by the presence of the 'Next' button)
            val onboardingVisible = device.wait(Until.hasObject(By.text("Next")), 5_000)
            if (onboardingVisible) {
                // Step 1: FirstStep -> Click "Next"
                device.waitAndClick(By.text("Next"))
                device.waitForIdle()

                // Step 2: ThemeStep -> Click "Next"
                device.waitAndClick(By.text("Next"))
                device.waitForIdle()

                // Step 3: StorageStep -> Click "Select" to open system SAF directory picker
                device.waitAndClick(By.text("Select"))
                device.waitForIdle()

                // SAF Picker: Click "Use this folder" confirmation button
                device.waitAndClickFallback(
                    By.res("android:id/button1"),
                    By.textContains("Use this folder")
                )
                device.waitForIdle()

                // System Dialog: Click "Allow" confirmation dialog
                device.waitAndClickFallback(
                    By.res("android:id/button1"),
                    By.textContains("Allow")
                )
                device.waitForIdle()

                // Back in app, StorageStep is completed -> Click "Next"
                device.waitAndClick(By.text("Next"))
                device.waitForIdle()

                // Step 4: PermissionStep -> Click "Finish"
                device.waitAndClick(By.text("Finish"))
                device.waitForIdle()
            }

            // Wait for the main Library screen tab text to render (timeout of 15 seconds)
            val libraryLoaded = device.wait(Until.hasObject(By.text("Library")), 15_000)
            if (!libraryLoaded) {
                throw RuntimeException("Library screen failed to load")
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

private fun UiDevice.waitAndClickFallback(byRes: BySelector, byText: BySelector) {
    val obj = wait(Until.findObject(byRes), 15_000)
        ?: wait(Until.findObject(byText), 5_000)
    obj?.click() ?: throw RuntimeException("Could not find button to click: $byRes or $byText")
}
