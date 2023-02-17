package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterFilter(
    val preferences: PreferencesHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
) {

    /** filters chapters based on the manga values */
    fun <T : Chapter> filterChapters(chapters: List<T>, manga: Manga): List<T> {
        val readEnabled = manga.readFilter(preferences) == Manga.CHAPTER_SHOW_READ
        val unreadEnabled = manga.readFilter(preferences) == Manga.CHAPTER_SHOW_UNREAD
        val downloadEnabled = manga.downloadedFilter(preferences) == Manga.CHAPTER_SHOW_DOWNLOADED
        val notDownloadEnabled =
            manga.downloadedFilter(preferences) == Manga.CHAPTER_SHOW_NOT_DOWNLOADED
        val bookmarkEnabled = manga.bookmarkedFilter(preferences) == Manga.CHAPTER_SHOW_BOOKMARKED
        val notBookmarkEnabled =
            manga.bookmarkedFilter(preferences) == Manga.CHAPTER_SHOW_NOT_BOOKMARKED

        // if none of the filters are enabled skip the filtering of them
        val filteredChapters = filterChaptersByScanlators(chapters, manga)
        return if (readEnabled || unreadEnabled || downloadEnabled || notDownloadEnabled || bookmarkEnabled || notBookmarkEnabled) {
            filteredChapters.filter {
                if (readEnabled && it.read.not() ||
                    (unreadEnabled && it.read) ||
                    (bookmarkEnabled && it.bookmark.not()) ||
                    (notBookmarkEnabled && it.bookmark) ||
                    (downloadEnabled && downloadManager.isChapterDownloaded(it, manga).not()) ||
                    (notDownloadEnabled && downloadManager.isChapterDownloaded(it, manga))
                ) {
                    return@filter false
                }
                return@filter true
            }
        } else {
            filteredChapters
        }
    }

    /** filter chapters for the reader */
    fun <T : Chapter> filterChaptersForReader(
        chapters: List<T>,
        manga: Manga,
        selectedChapter: T? = null,
    ): List<T> {
        var filteredChapters = filterChaptersByScanlators(chapters, manga)

        val blockedScanlator = preferences.blockedScanlators().get()

        if (blockedScanlator.isNotEmpty()) {
            filteredChapters = filteredChapters.filter { chp -> chp.scanlatorList().none { it in blockedScanlator } }
        }

        // if filter preferences are not enabled don't even filter
        if (!preferences.skipRead() && !preferences.skipFiltered() && !preferences.skipDuplicates().get()) {
            return filteredChapters
        }

        if (preferences.skipRead()) {
            filteredChapters = filteredChapters.filter { !it.read }
        }
        if (preferences.skipFiltered()) {
            filteredChapters = filterChapters(filteredChapters, manga)
        }

        if (preferences.skipDuplicates().get()) {
            filteredChapters = filteredChapters.groupBy { it.chapter_number }
                .map { (_, chapters) ->
                    chapters.find { it.id == selectedChapter?.id }
                        ?: chapters.find { it.scanlator == selectedChapter?.scanlator }
                        ?: chapters.find {
                            val mainScans = ChapterUtil.getScanlators(it.scanlator)
                            val currScans = ChapterUtil.getScanlators(selectedChapter?.scanlator)
                            if (currScans.isEmpty() || mainScans.isEmpty()) {
                                return@find false
                            }

                            mainScans.any { scanlator -> currScans.contains(scanlator) }
                        }
                        ?: chapters.first()
                }
        }

        // add the selected chapter to the list in case it was filtered out
        if (selectedChapter?.id != null) {
            val find = filteredChapters.find { it.id == selectedChapter.id }
            if (find == null) {
                val mutableList = filteredChapters.toMutableList()

                mutableList.add(selectedChapter)
                filteredChapters = mutableList.toList()
            }
        }

        return filteredChapters
    }

    /** filters chapters for scanlators */
    fun <T : Chapter> filterChaptersByScanlators(chapters: List<T>, manga: Manga): List<T> {
        return manga.filtered_scanlators?.let { filteredScanlatorString ->
            val filteredScanlators = ChapterUtil.getScanlators(filteredScanlatorString)
            chapters.filter {
                ChapterUtil.getScanlators(it.scanlator)
                    .none { group -> filteredScanlators.contains(group) }
            }
        } ?: chapters
    }
}
