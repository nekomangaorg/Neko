package org.nekomanga.domain.storage

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import tachiyomi.core.util.storage.DiskUtil

class StorageManager(private val context: Context, storagePreferences: StoragePreferences) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var baseDir: UniFile? = getBaseDir(storagePreferences.baseStorageDirectory().get())

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val baseDirChanges = _changes.receiveAsFlow().shareIn(scope, SharingStarted.Lazily, 1)

    init {
        storagePreferences
            .baseStorageDirectory()
            .changes()
            .drop(1)
            .distinctUntilChanged()
            .onEach { uri ->
                baseDir = getBaseDir(uri)
                baseDir?.let { parent ->
                    parent.createDirectory(BACKUP_DIR).also { it!!.createDirectory(AUTOMATIC_DIR) }
                    parent.createDirectory(SAVED_DIR).also {
                        it!!.createDirectory(COVER_DIR)
                        it.createDirectory(PAGES_DIR)
                    }
                    parent.createDirectory(DOWNLOADS_DIR).also {
                        DiskUtil.createNoMediaFile(it, context)
                    }
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    private fun getBaseDir(uri: String): UniFile? {
        return UniFile.fromUri(context, uri.toUri()).takeIf { it?.exists() == true }
    }

    fun getAutomaticBackupsDirectory(): UniFile? {
        return getBackupDirectory()?.createDirectory(AUTOMATIC_DIR)
    }

    fun getSavedDir(): UniFile? {
        return baseDir?.createDirectory(SAVED_DIR)
    }

    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_DIR)
    }

    fun getBackupDirectory(): UniFile? {
        return baseDir?.createDirectory(BACKUP_DIR)
    }

    fun getCoverDirectory(): UniFile? {
        return getSavedDir()?.createDirectory(COVER_DIR)
    }

    fun getPagesDirectory(): UniFile? {
        return getSavedDir()?.createDirectory(PAGES_DIR)
    }

    fun renamePagesAndCoverDirectory(from: String, to: String) {
        val oldName = DiskUtil.buildValidFilename(from)
        val newName = DiskUtil.buildValidFilename(to)

        val pagesDir = getPagesDirectory()
        val mangaPageDir = pagesDir?.findFile(oldName)
        mangaPageDir?.renameTo(newName)

        val coverDir = getCoverDirectory()
        val mangaCoverDir = coverDir?.findFile(oldName)
        mangaCoverDir?.renameTo(newName)
    }

    fun getCrashLogDirectory(): UniFile? {
        return baseDir?.createDirectory(CRASH_LOG_DIR)
    }

    companion object {
        const val BACKUP_DIR = "backup"
        const val AUTOMATIC_DIR = "automatic"
        const val COVER_DIR = "covers"
        const val PAGES_DIR = "pages"
        const val SAVED_DIR = "saved"
        const val DOWNLOADS_DIR = "downloads"
        const val CRASH_LOG_DIR = "crash-logs"
    }
}
