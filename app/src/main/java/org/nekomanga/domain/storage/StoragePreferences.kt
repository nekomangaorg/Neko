package org.nekomanga.domain.storage

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.util.storage.FolderProvider

class StoragePreferences(
    private val context: Context,
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    fun baseStorageDirectory() = preferenceStore.getString("storage_dir", folderProvider.path())

    fun backupInterval() = this.preferenceStore.getInt("backup_interval", 12)

    fun lastAutoBackupTimestamp() =
        preferenceStore.getLong(Preference.appStateKey("last_auto_backup_timestamp"), 0L)

    fun baseStorageDirectoryAsUniFile() =
        UniFile.fromUri(context, baseStorageDirectory().get().toUri())!!

    companion object {
        const val BACKUP_DIR = "backup"
        const val AUTOMATIC_DIR = "automatic"
        const val COVER_DIR = "covers"
        const val PAGES_DIR = "pages"
        const val DOWNLOADS_DIR = "downloads"
    }
}
