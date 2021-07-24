package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import com.elvishew.xlog.XLog
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CATEGORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CHAPTER_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_HISTORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_TRACK_MASK
import eu.kanade.tachiyomi.data.backup.full.models.Backup
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.full.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupManga
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.full.models.BackupTracking
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.sink
import uy.kohesive.injekt.injectLazy
import kotlin.math.max

class FullBackupManager(val context: Context) {

    internal val databaseHelper: DatabaseHelper by injectLazy()
    internal val sourceManager: SourceManager by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    protected val preferences: PreferencesHelper by injectLazy()

    val parser = ProtoBuf

    /**
     * Create backup Json file from database
     *
     * @param uri path of Uri
     * @param isJob backup called from job
     */
    fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String {
        // Create root object
        var backup: Backup? = null

        databaseHelper.inTransaction {
            val databaseManga = databaseHelper.getFavoriteMangaList().executeAsBlocking()

            backup = Backup(
                backupManga(databaseManga, flags),
                backupCategories(),
            )
        }

        try {
            val file: UniFile = (
                if (isJob) {
                    // Get dir of file and create
                    var dir = UniFile.fromUri(context, uri)
                    dir = dir.createDirectory("automatic")

                    // Delete older backups
                    val numberOfBackups = preferences.numberOfBackups().get()!!
                    val backupRegex = Regex("""neko_\d+-\d+-\d+_\d+-\d+.proto.gz""")
                    dir.listFiles { _, filename -> backupRegex.matches(filename) }
                        .orEmpty()
                        .sortedByDescending { it.name }
                        .drop(numberOfBackups - 1)
                        .forEach { it.delete() }

                    // Create new file to place backup
                    dir.createFile(BackupFull.getDefaultFilename())
                } else {
                    UniFile.fromUri(context, uri)
                }
                )
                ?: throw Exception("Couldn't create backup file")

            val byteArray = parser.encodeToByteArray(BackupSerializer, backup!!)
            file.openOutputStream().sink().gzip().buffer().use { it.write(byteArray) }
            return file.uri.toString()
        } catch (e: Exception) {
            XLog.e(e)
            throw e
        }
    }

    private fun backupManga(mangaList: List<Manga>, flags: Int): List<BackupManga> {
        return mangaList.map {
            backupMangaObject(it, flags)
        }
    }

    /**
     * Backup the categories of library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private fun backupCategories(): List<BackupCategory> {
        return databaseHelper.getCategories()
            .executeAsBlocking()
            .map { BackupCategory.copyFrom(it) }
    }

    /**
     * Convert a manga to Json
     *
     * @param manga manga that gets converted
     * @param options options for the backup
     * @return [BackupManga] containing manga in a serializable form
     */
    private fun backupMangaObject(manga: Manga, options: Int): BackupManga {
        // Entry for this manga
        val mangaObject = BackupManga.copyFrom(manga)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val chapters = databaseHelper.getChapters(manga).executeAsBlocking()
            if (chapters.isNotEmpty()) {
                mangaObject.chapters = chapters.map { BackupChapter.copyFrom(it) }
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForManga = databaseHelper.getCategoriesForManga(manga).executeAsBlocking()
            if (categoriesForManga.isNotEmpty()) {
                mangaObject.categories = categoriesForManga.mapNotNull { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = databaseHelper.getTracks(manga).executeAsBlocking()
            if (tracks.isNotEmpty()) {
                mangaObject.tracking = tracks.map { BackupTracking.copyFrom(it) }
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyForManga = databaseHelper.getHistoryByMangaId(manga.id!!).executeAsBlocking()
            if (historyForManga.isNotEmpty()) {
                val history = historyForManga.mapNotNull { history ->
                    val url = databaseHelper.getChapter(history.chapter_id).executeAsBlocking()?.url
                    url?.let { BackupHistory(url, history.last_read) }
                }
                if (history.isNotEmpty()) {
                    mangaObject.history = history
                }
            }
        }

        return mangaObject
    }

    fun restoreMangaNoFetch(manga: Manga, dbManga: Manga) {
        manga.id = dbManga.id
        manga.copyFrom(dbManga)
        manga.initialized = false
        manga.favorite = true
        databaseHelper.insertManga(manga).executeAsBlocking().insertedId()
    }

    /**
     * Fetches manga information
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @return Updated manga info.
     */
    suspend fun restoreMangaFetch(source: Source, manga: Manga): Manga {
        return withContext(Dispatchers.IO) {
            val networkManga = source.fetchMangaDetails(manga)
            manga.copyFrom(networkManga)
            manga.favorite = true
            manga.initialized = true
            manga.id = databaseHelper.insertManga(manga).executeAsBlocking().insertedId()
            manga
        }
    }

    /**
     * Restore the categories from Json
     *
     * @param backupCategories list containing categories
     */
    internal fun restoreCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = databaseHelper.getCategories().executeAsBlocking()

        // Iterate over them
        backupCategories.map { it.getCategoryImpl() }.forEach { category ->
            // Used to know if the category is already in the db
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.name == dbCategory.name) {
                    category.id = dbCategory.id
                    found = true
                    break
                }
            }
            // If the category isn't in the db, remove the id and insert a new category
            // Store the inserted id in the category
            if (!found) {
                // Let the db assign the id
                category.id = null
                val result = databaseHelper.insertCategory(category).executeAsBlocking()
                category.id = result.insertedId()?.toInt()
            }
        }
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal fun restoreCategoriesForManga(
        manga: Manga,
        categories: List<Int>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = databaseHelper.getCategories().executeAsBlocking()
        val mangaCategoriesToUpdate = ArrayList<MangaCategory>(categories.size)
        categories.forEach { backupCategoryOrder ->
            backupCategories.firstOrNull {
                it.order == backupCategoryOrder
            }?.let { backupCategory ->
                dbCategories.firstOrNull { dbCategory ->
                    dbCategory.name == backupCategory.name
                }?.let { dbCategory ->
                    mangaCategoriesToUpdate += MangaCategory.create(manga, dbCategory)
                }
            }
        }

        // Update database
        if (mangaCategoriesToUpdate.isNotEmpty()) {
            databaseHelper.deleteOldMangaListCategories(listOf(manga)).executeAsBlocking()
            databaseHelper.insertMangaListCategories(mangaCategoriesToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    internal fun restoreHistoryForManga(history: List<BackupHistory>) {
        // List containing history to be updated
        val historyToBeUpdated = ArrayList<History>(history.size)
        for ((url, lastRead) in history) {
            val dbHistory = databaseHelper.getHistoryByChapterUrl(url).executeAsBlocking()
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory.apply {
                    last_read = max(lastRead, dbHistory.last_read)
                }
                historyToBeUpdated.add(dbHistory)
            } else {
                // If not in database create
                databaseHelper.getChapter(url).executeAsBlocking()?.let {
                    val historyToAdd = History.create(it).apply {
                        last_read = lastRead
                    }
                    historyToBeUpdated.add(historyToAdd)
                }
            }
        }
        databaseHelper.updateHistoryLastRead(historyToBeUpdated).executeAsBlocking()
    }

    /**
     * Restores the sync of a manga.
     *
     * @param manga the manga whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    internal fun restoreTrackForManga(manga: Manga, tracks: List<Track>) {
        // Fix foreign keys with the current manga id
        tracks.map { it.manga_id = manga.id!! }

        val validTracks =
            tracks.filter { it.sync_id == TrackManager.MYANIMELIST || it.sync_id == TrackManager.ANILIST || it.sync_id == TrackManager.KITSU }

        if (validTracks.isEmpty()) {
            // always create an mdlist tracker
            val track = trackManager.mdList.createInitialTracker(manga)
            databaseHelper.insertTrack(track).executeAsBlocking()
        }

        // Get tracks from database
        val dbTracks = databaseHelper.getTracks(manga).executeAsBlocking()
        val trackToUpdate = mutableListOf<Track>()

        validTracks.forEach { track ->
            val service = trackManager.getService(track.sync_id)
            if (service != null && service.isLogged()) {
                var isInDatabase = false
                for (dbTrack in dbTracks) {
                    if (track.sync_id == dbTrack.sync_id) {
                        // The sync is already in the db, only update its fields
                        if (track.media_id != dbTrack.media_id) {
                            dbTrack.media_id = track.media_id
                        }
                        if (track.library_id != dbTrack.library_id) {
                            dbTrack.library_id = track.library_id
                        }
                        dbTrack.last_chapter_read =
                            max(dbTrack.last_chapter_read, track.last_chapter_read)
                        isInDatabase = true
                        trackToUpdate.add(dbTrack)
                        break
                    }
                }
                if (!isInDatabase) {
                    // Insert new sync. Let the db assign the id
                    track.id = null
                    trackToUpdate.add(track)
                }
            }
        }
        // Update database
        if (trackToUpdate.isNotEmpty()) {
            databaseHelper.insertTracks(trackToUpdate).executeAsBlocking()
        }
    }

    internal fun restoreChaptersForMangaOffline(manga: Manga, chapters: List<Chapter>) {
        val dbChapters = databaseHelper.getChapters(manga).executeAsBlocking()

        chapters.forEach { chapter ->
            val dbChapter = dbChapters.find { it.url == chapter.url }
            if (dbChapter != null) {
                chapter.id = dbChapter.id
                chapter.copyFrom(dbChapter)
                if (dbChapter.read && !chapter.read) {
                    chapter.read = dbChapter.read
                    chapter.last_page_read = dbChapter.last_page_read
                } else if (chapter.last_page_read == 0 && dbChapter.last_page_read != 0) {
                    chapter.last_page_read = dbChapter.last_page_read
                }
                if (!chapter.bookmark && dbChapter.bookmark) {
                    chapter.bookmark = dbChapter.bookmark
                }
            } else {
                chapter.mangadex_chapter_id = MdUtil.getChapterId(chapter.url)
            }
            chapter.manga_id = manga.id
        }

        val newChapters = chapters.groupBy { it.id != null }
        newChapters[true]?.let { databaseHelper.updateKnownChaptersBackup(it).executeAsBlocking() }
        newChapters[false]?.let { databaseHelper.insertChapters(it).executeAsBlocking() }
    }

    internal fun getMangaFromDatabase(manga: Manga): Manga? =
        databaseHelper.getManga(manga.url, manga.source).executeAsBlocking()

    internal fun insertManga(manga: Manga): Long? =
        databaseHelper.insertManga(manga).executeAsBlocking().insertedId()
}
