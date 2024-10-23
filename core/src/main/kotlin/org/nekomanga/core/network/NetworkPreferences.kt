package org.nekomanga.core.network

import tachiyomi.core.preference.PreferenceStore

class NetworkPreferences(
    private val preferenceStore: PreferenceStore,
    private val verboseLogging: Boolean = false,
) {

    fun verboseLogging() = this.preferenceStore.getBoolean("verbose_logging")

    fun dohProvider() = this.preferenceStore.getInt("doh_provider", -1)
}
