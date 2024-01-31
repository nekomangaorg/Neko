package org.nekomanga.domain.backup

import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore

class BackupPreferences(private val preferenceStore: PreferenceStore) {
    fun backupInterval() = this.preferenceStore.getInt("backup_interval", 12)

    fun lastAutoBackupTimestamp() =
        preferenceStore.getLong(Preference.appStateKey("last_auto_backup_timestamp"), 0L)
}
