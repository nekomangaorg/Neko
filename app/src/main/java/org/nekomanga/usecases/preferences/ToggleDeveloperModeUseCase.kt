package org.nekomanga.usecases.preferences

import eu.kanade.tachiyomi.data.preference.PreferencesHelper

class ToggleDeveloperModeUseCase(private val preferences: PreferencesHelper) {
    private var versionClickCount = 0
    private var lastVersionClickTime = 0L

    operator fun invoke(): Boolean? {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastVersionClickTime > DEV_MODE_CLICK_TIMEOUT_MS) {
            versionClickCount = 0
        }
        lastVersionClickTime = currentTime
        versionClickCount++

        if (versionClickCount == DEV_MODE_CLICK_COUNT) {
            versionClickCount = 0
            val newValue = !preferences.developerMode().get()
            preferences.developerMode().set(newValue)
            return newValue
        }
        return null
    }

    companion object {
        private const val DEV_MODE_CLICK_COUNT = 7
        private const val DEV_MODE_CLICK_TIMEOUT_MS = 500L
    }
}
