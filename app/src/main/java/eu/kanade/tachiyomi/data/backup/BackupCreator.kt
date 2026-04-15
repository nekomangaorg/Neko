package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
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
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.model.toCategory
import org.nekomanga.data.database.model.toChapter
import org.nekomanga.data.database.model.toHistory
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.model.toMangaCategory
import org.nekomanga.data.database.model.toMergeManga
import org.nekomanga.data.database.model.toTrack
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.MergeRepositoryImpl
import org.nekomanga.data.database.repository.TrackRepositoryImpl
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class BackupCreator(val context: Context) {

    private val mangaRepository: MangaRepositoryImpl by injectLazy()
    private val chapterRepository: ChapterRepositoryImpl by injectLazy()
    private val categoryRepository: CategoryRepositoryImpl by injectLazy()
    private val trackRepository: TrackRepositoryImpl by injectLazy()
    private val mergeRepository: MergeRepositoryImpl by injectLazy()
    private val appDatabase: AppDatabase by injectLazy()

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
    suspend fun createBackup(uri: Uri, flags: Int, isAutoBackup: Boolean): String {
        // Create root object
        var backup: Backup? = null

        appDatabase.withTransaction {
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

    private suspend fun backupManga(mangaList: List<Manga>, flags: Int): List<BackupManga> {
        val allCategories =
            if (flags and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY)
                categoryRepository
                    .getAllCategoriesList()
                    .map { it.toCategory() }
                    .associateBy { it.id }
            else emptyMap()

        return mangaList.chunked(500).flatMap { chunk ->
            val mangaIds = chunk.mapNotNull { it.id }

            // Pre-fetch all dependencies for the list of mangas
            val mergeMangaMap =
                mergeRepository
                    .getMergeMangaList(mangaIds)
                    .map { it.toMergeManga() }
                    .groupBy { it.mangaId }

            val chaptersMap =
                if (
                    flags and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER ||
                        flags and BACKUP_HISTORY_MASK == BACKUP_HISTORY
                ) {
                    chapterRepository
                        .getChaptersForMangas(mangaIds)
                        .map { it.toChapter() }
                        .groupBy { it.manga_id }
                } else {
                    emptyMap()
                }

            val categoriesMap =
                if (flags and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
                    categoryRepository
                        .getMangaCategoriesList(mangaIds)
                        .map { it.toMangaCategory() }
                        .groupBy { it.manga_id }
                } else {
                    emptyMap()
                }

            val tracksMap =
                if (flags and BACKUP_TRACK_MASK == BACKUP_TRACK) {
                    trackRepository
                        .getTracksForMangas(mangaIds)
                        .map { it.toTrack() }
                        .groupBy { it.manga_id }
                } else {
                    emptyMap()
                }

            val historyMap =
                if (flags and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
                    // We need history entries matching chapters from these mangas.
                    // getHistoryByMangaIds fetches history matching chapter_id linked to the manga
                    chapterRepository
                        .getHistoryByMangaIds(mangaIds)
                        .map { it.toHistory() }
                        .groupBy {
                            // It isn't trivial to group by mangaId since history object only
                            // contains
                            // chapter_id.
                            // However history object does not have mangaId so grouping by
                            // chapter_id
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
    private suspend fun backupCategories(): List<BackupCategory> {
        return categoryRepository
            .getAllCategoriesList()
            .map { it.toCategory() }
            .map { BackupCategory.copyFrom(it) }
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

    internal suspend fun getFavoriteManga(): List<Manga> =
        mangaRepository.getFavoriteMangaListSync().map { it.toManga() }

    internal suspend fun getReadManga(): List<Manga> =
        mangaRepository.getReadNotInLibraryMangasSync().map { it.toManga() }
}
