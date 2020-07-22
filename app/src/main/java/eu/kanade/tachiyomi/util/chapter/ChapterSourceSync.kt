package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.getChapterNum
import eu.kanade.tachiyomi.source.model.getVolumeNum
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import java.util.Date
import java.util.TreeSet

/**
 * Helper method for syncing the list of chapters from the source with the ones from the database.
 *
 * @param db the database.
 * @param rawSourceChapters a list of chapters from the source.
 * @param manga the manga of the chapters.
 * @param source the source of the chapters.
 * @return a pair of new insertions and deletions.
 */
fun syncChaptersWithSource(
    db: DatabaseHelper,
    rawSourceChapters: List<SChapter>,
    manga: Manga
): Pair<List<Chapter>, List<Chapter>> {

    // Chapters from db.
    val dbChapters = db.getChapters(manga).executeAsBlocking()
    var copyOfRawSource = rawSourceChapters.toList()
    if (manga.mergeMangaUrl != null) {
        val dexChapters = copyOfRawSource.filter { !it.isMergedChapter() }.toMutableList()
        val mergedChapters = copyOfRawSource.filter { it.isMergedChapter() }

        val allVol1 = dexChapters.all { it.getVolumeNum() == 1 }
        val allNull = mergedChapters.all { it.getVolumeNum() == null }


        mergedChapters.forEach { it ->
            val exists = dexChapters.filter { dex ->
                when (allVol1 && allNull) {
                    true -> true
                    false -> it.getVolumeNum() == dex.getVolumeNum()
                }
            }
                .find { dex -> dex.getChapterNum() == it.getChapterNum() }
            if (exists == null) {
                dexChapters.add(it)
            }
        }
        val sorter = when (allNull && allVol1) {
            true -> compareByDescending { it.getChapterNum() }
            false -> compareByDescending<SChapter> { it.getVolumeNum() }.thenByDescending { it.getChapterNum() }
        }

        copyOfRawSource = dexChapters.sortedWith(sorter)
    }

    val sourceChapters = copyOfRawSource.mapIndexed { i, sChapter ->
        Chapter.create().apply {
            copyFrom(sChapter)
            manga_id = manga.id
            source_order = i
        }
    }

    // Chapters from the source not in db.
    val toAdd = mutableListOf<Chapter>()

    // Chapters whose metadata have changed.
    val toChange = mutableListOf<Chapter>()

    for (sourceChapter in sourceChapters) {
        val dbChapter = dbChapters.find {
            if (sourceChapter.isMergedChapter() && it.isMergedChapter()) {
                it.url == sourceChapter.url
            } else if (sourceChapter.isMergedChapter().not() && it.isMergedChapter().not()) {
                (it.mangadex_chapter_id.isNotBlank() && it.mangadex_chapter_id == sourceChapter.mangadex_chapter_id) ||
                    MdUtil.getChapterId(it.url) == sourceChapter.mangadex_chapter_id
            } else {
                false
            }

        }

        // Add the chapter if not in db already, or update if the metadata changed.
        if (dbChapter == null) {
            toAdd.add(sourceChapter)
        } else {

            ChapterRecognition.parseChapterNumber(sourceChapter, manga)

            if (shouldUpdateDbChapter(dbChapter, sourceChapter)) {
                dbChapter.scanlator = sourceChapter.scanlator
                dbChapter.name = sourceChapter.name
                dbChapter.vol = sourceChapter.vol
                dbChapter.chapter_txt = sourceChapter.chapter_txt
                dbChapter.chapter_title = sourceChapter.chapter_title
                dbChapter.date_upload = sourceChapter.date_upload
                dbChapter.chapter_number = sourceChapter.chapter_number
                dbChapter.mangadex_chapter_id = sourceChapter.mangadex_chapter_id
                dbChapter.language = sourceChapter.language
                toChange.add(dbChapter)
            }
        }
    }

    // Recognize number for new chapters.
    toAdd.forEach {
        ChapterRecognition.parseChapterNumber(it, manga)
    }

    // Chapters from the db not in the source.
    val toDelete = dbChapters.filterNot { dbChapter ->
        sourceChapters.any { sourceChapter ->
            dbChapter.mangadex_chapter_id == sourceChapter.mangadex_chapter_id ||
                dbChapter.url == sourceChapter.url
        }
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
                val c = toAdd[i]
                c.date_fetch = now++
                // Try to mark already read chapters as read when the source deletes them
                if (c.isRecognizedNumber && c.chapter_number in deletedReadChapterNumbers) {
                    c.read = true
                }
                if (c.isRecognizedNumber && c.chapter_number in deletedChapterNumbers) {
                    readded.add(c)
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

        // Fix order in source.
        db.fixChaptersSourceOrder(sourceChapters).executeAsBlocking()

        // Set this manga as updated since chapters were changed
        val newestChapter = db.getChapters(manga).executeAsBlocking().maxBy { it.date_upload }
        val dateFetch = newestChapter?.date_upload ?: manga.last_update
        if (dateFetch == 0L) {
            if (toAdd.isNotEmpty())
                manga.last_update = Date().time
        } else manga.last_update = dateFetch
        db.updateLastUpdated(manga).executeAsBlocking()
    }

    return Pair(toAdd.subtract(readded).toList(), toDelete - readded)
}

// checks if the chapter in db needs updated
private fun shouldUpdateDbChapter(dbChapter: Chapter, sourceChapter: SChapter): Boolean {
    return dbChapter.scanlator != sourceChapter.scanlator ||
        dbChapter.name != sourceChapter.name ||
        dbChapter.date_upload != sourceChapter.date_upload ||
        dbChapter.chapter_number != sourceChapter.chapter_number ||
        dbChapter.vol != sourceChapter.vol ||
        dbChapter.chapter_title != sourceChapter.chapter_title ||
        dbChapter.chapter_txt != sourceChapter.chapter_txt ||
        dbChapter.mangadex_chapter_id != sourceChapter.mangadex_chapter_id ||
        dbChapter.language != sourceChapter.language
}
