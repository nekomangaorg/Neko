package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isLocalSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import java.util.Date
import java.util.TreeSet
import kotlinx.coroutines.runBlocking
import org.nekomanga.constants.Constants
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import tachiyomi.core.util.storage.nameWithoutExtension
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Helper method for syncing the list of chapters from the source with the ones from the database.
 *
 * @param db the database.
 * @param rawSourceChapters a list of chapters from the source.
 * @param manga the manga of the chapters.
 * @param errorFromMerged whether there is an error is from a merged source
 * @param readFromMerged a set of merged chapters that have a read status
 * @return a pair of new insertions and deletions.
 */
fun syncChaptersWithSource(
    db: DatabaseHelper,
    rawSourceChapters: List<SChapter>,
    manga: Manga,
    errorFromMerged: Boolean = false,
    readFromMerged: Set<String> = emptySet(),
): Pair<List<Chapter>, List<Chapter>> {
    val downloadManager: DownloadManager = Injekt.get()
    val libraryPreferences: LibraryPreferences = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()

    // Chapters from db.
    var dbChapters = db.getChapters(manga).executeAsBlocking()

    val localChapterLookupEnabled = libraryPreferences.enableLocalChapters().get()
    var finalRawSourceChapters = rawSourceChapters

    // Check for local chapter to add

    if (localChapterLookupEnabled) {

        TimberKt.d { "local chapter enabled, checking for orphans" }

        val allDownloadsMap =
            downloadManager.getAllDownloads(manga).associateBy { it.name }.toMutableMap()

        if (allDownloadsMap.isNotEmpty()) {
            dbChapters.forEach { dbChapter ->
                if (allDownloadsMap.isNotEmpty()) {
                    if (
                        (!dbChapter.isLocalSource() &&
                            downloadManager.isChapterDownloaded(dbChapter, manga)) ||
                            (dbChapter.isLocalSource() &&
                                downloadManager.isChapterDownloaded(dbChapter, manga, true))
                    ) {
                        val validName =
                            downloadManager.downloadedChapterName(dbChapter).firstOrNull {
                                it in allDownloadsMap
                            }
                        if (validName != null) {
                            allDownloadsMap.remove(validName)
                        }
                    } else if (dbChapter.isLocalSource()) { // means its not downloaded currently
                        db.deleteChapter(dbChapter).executeAsBlocking()
                    }
                }
            }
        }

        val allDownloads = allDownloadsMap.values.toList()

        finalRawSourceChapters =
            if (allDownloads.isNotEmpty()) {
                val localSourceChapters =
                    allDownloads.map { file ->
                        val chapterName = file.nameWithoutExtension!!.substringAfter("_")
                        val dateUploaded = file.lastModified()
                        val fileNameSuffix = file.name?.substringAfter("_")
                        val expectedFileName =
                            DiskUtil.buildValidFilename(
                                "${Constants.LOCAL_SOURCE}_${fileNameSuffix}"
                            )
                        if (file.name != expectedFileName) {
                            file.renameTo(expectedFileName)
                        }

                        SChapter.create().apply {
                            url = "${Constants.LOCAL_SOURCE}/$file"
                            name = chapterName
                            chapter_txt = chapterName
                            scanlator = Constants.LOCAL_SOURCE
                            date_upload = dateUploaded
                            isUnavailable = true
                        }
                    }
                downloadManager.refreshCache()
                listOf(rawSourceChapters, localSourceChapters)
                    .mergeSorted(
                        compareBy<SChapter> { getChapterNum(it) != null }
                            .thenBy { getChapterNum(it) }
                    )
            } else {
                val localSourceChapters =
                    dbChapters
                        .filter {
                            it.isLocalSource() && downloadManager.isChapterDownloaded(it, manga)
                        }
                        .map { dbChapter -> SChapter.create().apply { copyFrom(dbChapter) } }
                listOf(rawSourceChapters, localSourceChapters)
                    .mergeSorted(
                        compareBy<SChapter> { getChapterNum(it) != null }
                            .thenBy { getChapterNum(it) }
                    )
            }
    }

    val sourceChapters =
        finalRawSourceChapters.mapIndexed { i, sChapter ->
            Chapter.create().apply {
                copyFrom(sChapter)
                manga_id = manga.id
                source_order = i
            }
        }

    val sortedChapters = reorderChapters(sourceChapters)
    val finalChapters =
        sortedChapters.mapIndexed { i, chapter ->
            Chapter.create().apply {
                copyFrom(chapter)
                TimberKt.d {
                    "ChapterSourceSync ${this.scanlator} ${this.chapter_txt} sourceOrder=${this.source_order} smartOrder=${i}"
                }
                smart_order = i
            }
        }

    dbChapters = db.getChapters(manga).executeAsBlocking()

    val dbChaptersByUrl = dbChapters.associateBy { it.url }

    // Chapters from the source not in db.
    val toAdd = mutableListOf<Chapter>()

    // Chapters whose metadata have changed.
    val toChange = mutableListOf<Chapter>()

    // Read chapters to push to  remote hosted source.
    val toSync = mutableListOf<Chapter>()

    for (sourceChapter in finalChapters) {
        val dbChapter = dbChaptersByUrl[sourceChapter.url]

        // Add the chapter if not in db already, or update if the metadata changed.

        if (dbChapter == null) {
            val chapter = sourceChapter.apply { if (this.url in readFromMerged) this.read = true }
            toAdd.add(chapter)
        } else {
            ChapterRecognition.parseChapterNumber(sourceChapter, manga)
            val isMergedRead = sourceChapter.url in readFromMerged
            if (
                shouldUpdateDbChapter(dbChapter, sourceChapter) || (!dbChapter.read && isMergedRead)
            ) {
                if (
                    dbChapter.name != sourceChapter.name &&
                        downloadManager.isChapterDownloaded(dbChapter, manga)
                ) {
                    downloadManager.renameChapter(manga, dbChapter, sourceChapter)
                }
                dbChapter.scanlator = sourceChapter.scanlator
                dbChapter.uploader = sourceChapter.uploader
                dbChapter.name = sourceChapter.name
                dbChapter.vol = sourceChapter.vol
                dbChapter.chapter_txt = sourceChapter.chapter_txt
                dbChapter.chapter_title = sourceChapter.chapter_title
                dbChapter.date_upload = sourceChapter.date_upload
                dbChapter.chapter_number = sourceChapter.chapter_number
                dbChapter.mangadex_chapter_id = sourceChapter.mangadex_chapter_id
                dbChapter.language = sourceChapter.language
                dbChapter.isUnavailable = sourceChapter.isUnavailable
                dbChapter.source_order = sourceChapter.source_order
                dbChapter.smart_order = sourceChapter.smart_order
                if (isMergedRead) dbChapter.read = true
                toChange.add(dbChapter)
            }
            if (!isMergedRead && dbChapter.read) {
                toSync.add(dbChapter)
            }
        }
    }
    if (preferences.readingSync().get()) {
        runBlocking { Injekt.get<StatusHandler>().markMergedChaptersStatus(toSync, true) }
    }

    // Recognize number for new chapters.
    toAdd.forEach { ChapterRecognition.parseChapterNumber(it, manga) }

    // Chapters from the db not in the source.
    var toDelete =
        dbChapters.filterNot { dbChapter ->
            // ignore to delete when there is a site error
            if (dbChapter.isMergedChapter() && errorFromMerged) {
                true
            } else if (dbChapter.isLocalSource()) {
                downloadManager.isChapterDownloaded(dbChapter, manga, true)
            } else {
                sourceChapters.any { sourceChapter ->
                    dbChapter.mangadex_chapter_id == sourceChapter.mangadex_chapter_id
                }
            }
        }

    val dupes =
        dbChapters
            .groupBy { it.url }
            .filter { entry -> entry.value.size > 1 }
            .map { entry -> entry.value.firstOrNull { !it.read } ?: entry.value.first() }
            .toMutableList()
    if (dupes.isNotEmpty()) {
        dupes.addAll(toDelete)
        toDelete = dupes.toList()
    }

    // Return if there's nothing to add, delete or change, avoiding unnecessary db transactions.
    if (toAdd.isEmpty() && toDelete.isEmpty() && toChange.isEmpty()) {
        val topChapters = dbChapters.sortedByDescending { it.date_upload }.take(4)
        val newestDate = topChapters.getOrNull(0)?.date_upload ?: 0L

        // Recalculate update rate if unset and enough chapters are present
        if (manga.next_update == 0L && topChapters.size > 1) {
            var delta = 0L
            for (i in 0 until topChapters.size - 1) {
                delta += (topChapters[i].date_upload - topChapters[i + 1].date_upload)
            }
            delta /= topChapters.size - 1
            manga.next_update = newestDate + delta
            db.updateNextUpdated(manga).executeAsBlocking()
        }

        if (newestDate != 0L && newestDate != manga.last_update) {
            manga.last_update = newestDate
            db.updateLastUpdated(manga).executeAsBlocking()
        }
        return Pair(emptyList(), emptyList())
    }

    val readded = mutableListOf<Chapter>()

    db.inTransaction {
        val deletedChapterNumbers = TreeSet<Float>()
        val deletedReadChapterNumbers = TreeSet<Float>()
        if (toDelete.isNotEmpty()) {
            for (c in toDelete) {
                if (c.read) {
                    deletedReadChapterNumbers.add(c.chapter_number)
                }
                deletedChapterNumbers.add(c.chapter_number)
            }
            db.deleteChapters(toDelete).executeAsBlocking()
        }

        if (toAdd.isNotEmpty()) {
            // Set the date fetch for new items in reverse order to allow another sorting method.
            // Sources MUST return the chapters from most to less recent, which is common.
            var now = Date().time

            for (i in toAdd.indices.reversed()) {
                val chapter = toAdd[i]
                chapter.date_fetch = now++
                if (chapter.isRecognizedNumber && chapter.chapter_number in deletedChapterNumbers) {
                    // Try to mark already read chapters as read when the source deletes them
                    if (chapter.chapter_number in deletedReadChapterNumbers) {
                        chapter.read = true
                    }
                    // Try to to use the fetch date it originally had to not pollute 'Updates' tab
                    toDelete
                        .filter { it.chapter_number == chapter.chapter_number }
                        .minByOrNull { it.date_fetch }
                        ?.let { chapter.date_fetch = it.date_fetch }

                    readded.add(chapter)
                }
            }
            val chapters = db.insertChapters(toAdd).executeAsBlocking()
            toAdd.forEach { chapter ->
                chapter.id = chapters.results().getValue(chapter).insertedId()
            }
        }

        if (toChange.isNotEmpty()) {
            db.insertChapters(toChange).executeAsBlocking()
        }
        val topChapters =
            db.getChapters(manga).executeAsBlocking().sortedByDescending { it.date_upload }.take(4)
        // Recalculate next update since chapters were changed
        if (topChapters.size > 1) {
            var delta = 0L
            for (i in 0 until topChapters.size - 1) {
                delta += (topChapters[i].date_upload - topChapters[i + 1].date_upload)
            }
            delta /= topChapters.size - 1
            manga.next_update = topChapters[0].date_upload + delta
            db.updateNextUpdated(manga).executeAsBlocking()
        }

        // Set this manga as updated since chapters were changed
        val newestChapter = topChapters.getOrNull(0)
        val dateFetch = newestChapter?.date_upload ?: manga.last_update
        if (dateFetch == 0L) {
            if (toAdd.isNotEmpty()) {
                manga.last_update = Date().time
            }
        } else {
            manga.last_update = dateFetch
        }
        db.updateLastUpdated(manga).executeAsBlocking()
    }
    val newChapters = toAdd.subtract(readded.toSet()).toList().filter { !it.isUnavailable }

    return Pair(newChapters, toDelete - readded.toSet())
}

private fun bothMerged(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
    return dbChapter.isMergedChapter() && sourceChapter.isMergedChapter()
}

private fun bothLocal(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
    return dbChapter.isLocalSource() && sourceChapter.isLocalSource()
}

// checks if the chapter in db needs updated
private fun shouldUpdateDbChapter(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
    return dbChapter.scanlator != sourceChapter.scanlator ||
        dbChapter.uploader != sourceChapter.uploader ||
        dbChapter.name != sourceChapter.name ||
        dbChapter.date_upload != sourceChapter.date_upload ||
        dbChapter.chapter_number != sourceChapter.chapter_number ||
        dbChapter.vol != sourceChapter.vol ||
        dbChapter.chapter_title != sourceChapter.chapter_title ||
        dbChapter.chapter_txt != sourceChapter.chapter_txt ||
        dbChapter.mangadex_chapter_id != sourceChapter.mangadex_chapter_id ||
        dbChapter.language != sourceChapter.language ||
        dbChapter.isUnavailable != sourceChapter.isUnavailable ||
        dbChapter.source_order != sourceChapter.source_order ||
        dbChapter.smart_order != sourceChapter.smart_order
}
