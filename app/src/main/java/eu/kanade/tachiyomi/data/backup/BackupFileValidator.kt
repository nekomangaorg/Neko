package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.track.TrackManager
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupFileValidator(
    private val trackManager: TrackManager = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if manga cannot be found.
     * @return List of missing sources or missing trackers.
     */
    fun validate(context: Context, uri: Uri): Results {
        val backupManager = BackupManager(context)

        val backup = try {
            val backupString =
                context.contentResolver.openInputStream(uri)!!.source().gzip().buffer()
                    .use { it.readByteArray() }
            backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        if (backup.backupManga.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.backup_has_no_manga))
        }

        val trackers = backup.backupManga
            .flatMap { it.tracking }
            .map { it.syncId }
            .distinct()

        val hasDexEntries = backup.backupManga.any { backupManager.sourceManager.isMangadex(it.source) }

        val missingTrackers = trackers
            .mapNotNull { trackManager.getService(it) }
            .filter { !it.isLogged() }
            .map { context.getString(it.nameRes()) }
            .sorted()

        return Results(missingTrackers, !hasDexEntries)
    }

    data class Results(val missingTrackers: List<String>, val missingMangaDexEntries: Boolean)
}
