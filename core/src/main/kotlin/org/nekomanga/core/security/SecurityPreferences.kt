package org.nekomanga.core.security

import org.nekomanga.core.R
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum

class SecurityPreferences(private val preferenceStore: PreferenceStore) {

    fun useBiometrics() = this.preferenceStore.getBoolean("use_biometrics")

    fun incognitoMode() = this.preferenceStore.getBoolean("incognito_mode")

    fun lockAfter() = this.preferenceStore.getInt("lock_after")

    fun hideNotificationContent() = this.preferenceStore.getBoolean("hide_notification_content")

    fun lastUnlock() = this.preferenceStore.getLong("last_unlock")

    fun secureScreen() =
        this.preferenceStore.getEnum("secure_screen_v2", SecureScreenMode.INCOGNITO)

    enum class SecureScreenMode(val titleResId: Int) {
        ALWAYS(R.string.always),
        INCOGNITO(R.string.incognito_mode),
        NEVER(R.string.never),
    }
}
