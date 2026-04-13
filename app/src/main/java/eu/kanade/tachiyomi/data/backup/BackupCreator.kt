package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CATEGORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CHAPTER_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_HISTORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_READ_MANGA
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_READ_MANGA_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_TRACK_MASK
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupMergeManga
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import java.io.FileOutputStream
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.sink
import org.nekomanga.R
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class BackupCreator(val context: Context) {

    internal val databaseHelper: DatabaseHelper by injectLazy()
    internal val sourceManager: SourceManager by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    internal val storageManager: StorageManager by injectLazy()

    private val MAX_AUTO_BACKUPS: Int = 6

    val parser = ProtoBuf

    /**
     * Create backup Json file from database
     *
     * @param uri path of Uri
     * @param isAutoBackup backup called from scheduled backup job
     */
    fun createBackup(uri: Uri, flags: Int, isAutoBackup: Boolean): String {
        // Create root object
        var backup: Backup? = null

        databaseHelper.inTransaction {
            val databaseManga =
                getFavoriteManga() +
                    if (flags and BACKUP_READ_MANGA_MASK == BACKUP_READ_MANGA || isAutoBackup) {
                        getReadManga()
                    } else {
                        emptyList()
                    }

            if (databaseManga.isEmpty()) {
                throw NoLibraryManga()
            }

            backup = Backup(backupManga(databaseManga, flags), backupCategories())
        }

        var file: UniFile? = null
        try {
            file =
                (if (isAutoBackup) {
                    // Get dir of file and create
                    val dir = storageManager.getAutomaticBackupsDirectory()!!

                    // Delete older backups
                    dir.listFiles { _, filename -> Backup.filenameRegex.matches(filename) }
                        .orEmpty()
                        .sortedByDescending { it.name }
                        .drop(MAX_AUTO_BACKUPS - 1)
                        .forEach { it.delete() }

                    // Create new file to place backup
                    dir.createFile(Backup.getBackupFilename())
                } else {
                    UniFile.fromUri(context, uri)
                }) ?: throw Exception("Couldn't create backup file")

            if (!file.isFile) {
                throw IllegalStateException("Failed to get handle on file")
            }

            val byteArray = parser.encodeToByteArray(Backup.serializer(), backup!!)
            if (byteArray.isEmpty()) {
                throw IllegalStateException(context.getString(R.string.empty_backup_error))
            }

            file
                .openOutputStream()
                .also {
                    // Force overwrite old file
                    (it as? FileOutputStream)?.channel?.truncate(0)
                }
                .sink()
                .gzip()
                .buffer()
                .use { it.write(byteArray) }
            val fileUri = file.uri

            // Make sure it's a valid backup file
            BackupFileValidator().validate(context, fileUri)

            return fileUri.toString()
        } catch (e: Exception) {
            TimberKt.e(e) { "error creating backup file" }
            file?.delete()
            throw e
        }
    }

    private fun backupManga(mangaList: List<Manga>, flags: Int): List<BackupManga> {
        val allCategories =
            if (flags and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY)
                databaseHelper.getCategories().executeAsBlocking().associateBy { it.id }
            else emptyMap()

        return mangaList.chunked(500).flatMap { chunk ->
            val mangaIds = chunk.mapNotNull { it.id }

            // Pre-fetch all dependencies for the list of mangas
            val mergeMangaMap =
                databaseHelper.getMergeMangaList(mangaIds).executeAsBlocking().groupBy {
                    it.mangaId
                }

            val chaptersMap =
                if (
                    flags and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER ||
                        flags and BACKUP_HISTORY_MASK == BACKUP_HISTORY
                ) {
                    databaseHelper.getChapters(mangaIds).executeAsBlocking().groupBy { it.manga_id }
                } else {
                    emptyMap()
                }

            val categoriesMap =
                if (flags and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
                    databaseHelper.getMangaCategories(mangaIds).executeAsBlocking().groupBy {
                        it.manga_id
                    }
                } else {
                    emptyMap()
                }

            val tracksMap =
                if (flags and BACKUP_TRACK_MASK == BACKUP_TRACK) {
                    databaseHelper.getTracks(mangaIds).executeAsBlocking().groupBy { it.manga_id }
                } else {
                    emptyMap()
                }

            val historyMap =
                if (flags and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
                    // We need history entries matching chapters from these mangas.
                    // getHistoryByMangaIds fetches history matching chapter_id linked to the manga
                    databaseHelper.getHistoryByMangaIds(mangaIds).executeAsBlocking().groupBy {
                        // It isn't trivial to group by mangaId since history object only contains
                        // chapter_id.
                        // However history object does not have mangaId so grouping by chapter_id
                        // here
                        // and associating inside backupMangaObject.
                        it.chapter_id
                    }
                } else emptyMap()

            chunk.map {
                backupMangaObject(
                    it,
                    flags,
                    mergeMangaMap[it.id] ?: emptyList(),
                    chaptersMap[it.id] ?: emptyList(),
                    categoriesMap[it.id] ?: emptyList(),
                    allCategories,
                    tracksMap[it.id] ?: emptyList(),
                    historyMap,
                )
            }
        }
    }

    /**
     * Backup the categories of library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private fun backupCategories(): List<BackupCategory> {
        return databaseHelper.getCategories().executeAsBlocking().map {
            BackupCategory.copyFrom(it)
        }
    }

    /**
     * Convert a manga to Json
     *
     * @param manga manga that gets converted
     * @param options options for the backup
     * @return [BackupManga] containing manga in a serializable form
     */
    private fun backupMangaObject(
        manga: Manga,
        options: Int,
        mergeMangaList: List<MergeMangaImpl>,
        chapters: List<Chapter>,
        categoriesForManga: List<MangaCategory>,
        allCategories: Map<Int?, Category>,
        tracks: List<Track>,
        historyMap: Map<Long, List<History>>,
    ): BackupManga {
        // Entry for this manga
        val mangaObject = BackupManga.copyFrom(manga)

        if (mergeMangaList.isNotEmpty()) {
            mangaObject.mergeMangaList = mergeMangaList.map { BackupMergeManga.copyFrom(it) }
        }

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            if (chapters.isNotEmpty()) {
                mangaObject.chapters = chapters.map { BackupChapter.copyFrom(it) }
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            if (categoriesForManga.isNotEmpty()) {
                mangaObject.categories = categoriesForManga.mapNotNull {
                    allCategories[it.category_id]?.order
                }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            if (tracks.isNotEmpty()) {
                mangaObject.tracking = tracks.map { BackupTracking.copyFrom(it) }
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyForManga = chapters.flatMap { historyMap[it.id] ?: emptyList() }
            if (historyForManga.isNotEmpty()) {
                val historyChapters = chapters.associateBy { it.id }
                val history = historyForManga.mapNotNull { history ->
                    historyChapters[history.chapter_id]?.url?.let {
                        BackupHistory(it, history.last_read, history.time_read)
                    }
                }
                if (history.isNotEmpty()) {
                    mangaObject.history = history
                }
            }
        }

        return mangaObject
    }

    internal fun getFavoriteManga(): List<Manga> =
        databaseHelper.getFavoriteMangaList().executeAsBlocking()

    internal fun getReadManga(): List<Manga> =
        databaseHelper.getReadNotInLibraryMangas().executeAsBlocking()
}
