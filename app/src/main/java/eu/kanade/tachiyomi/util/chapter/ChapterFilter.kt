package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterFilter(
    val preferences: PreferencesHelper = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
) {

    /** filters chapters based on the manga values */
    fun <T : Chapter> filterChapters(chapters: List<T>, manga: Manga): List<T> {
        val readEnabled = manga.readFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_READ
        val unreadEnabled = manga.readFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_UNREAD
        val downloadEnabled =
            manga.downloadedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_DOWNLOADED
        val notDownloadEnabled =
            manga.downloadedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_NOT_DOWNLOADED
        val bookmarkEnabled =
            manga.bookmarkedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_BOOKMARKED
        val notBookmarkEnabled =
            manga.bookmarkedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_NOT_BOOKMARKED

        // if none of the filters are enabled skip the filtering of them
        val filteredChapters = filterChaptersByScanlators(chapters, manga, preferences)

        return if (
            readEnabled ||
                unreadEnabled ||
                downloadEnabled ||
                notDownloadEnabled ||
                bookmarkEnabled ||
                notBookmarkEnabled
        ) {
            filteredChapters.filter {
                return@filter !(readEnabled && !it.read ||
                    (unreadEnabled && it.read) ||
                    (bookmarkEnabled && !it.bookmark) ||
                    (notBookmarkEnabled && it.bookmark) ||
                    (downloadEnabled && !downloadManager.isChapterDownloaded(it, manga)) ||
                    (notDownloadEnabled && downloadManager.isChapterDownloaded(it, manga)))
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
        var filteredChapters = filterChaptersByScanlators(chapters, manga, preferences)

        // if filter preferences are not enabled don't even filter
        if (
            !readerPreferences.skipRead().get() &&
                !readerPreferences.skipFiltered().get() &&
                !readerPreferences.skipDuplicates().get()
        ) {
            return filteredChapters
        }

        if (readerPreferences.skipRead().get()) {
            filteredChapters = filteredChapters.filter { !it.read }
        }
        if (readerPreferences.skipFiltered().get()) {
            filteredChapters = filterChapters(filteredChapters, manga)
        }

        if (readerPreferences.skipDuplicates().get()) {
            filteredChapters =
                filteredChapters
                    .groupBy { it.chapter_number }
                    .map { (_, chapters) ->
                        chapters.find { it.id == selectedChapter?.id }
                            ?: chapters.find { it.scanlator == selectedChapter?.scanlator }
                            ?: chapters.find {
                                val mainScans = ChapterUtil.getScanlators(it.scanlator)
                                val currScans =
                                    ChapterUtil.getScanlators(selectedChapter?.scanlator)
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

    /**
     * filters chapters for scanlators, excludes globally blocked, unsupported and manga specific
     * filtered
     */
    fun <T : Chapter> filterChaptersByScanlators(
        chapters: List<T>,
        manga: Manga,
        preferences: PreferencesHelper,
    ): List<T> {
        val blockedGroups = preferences.blockedScanlators().get()
        val filteredGroupList = ChapterUtil.getScanlators(manga.filtered_scanlators)

        return chapters.filter {
            val groups = ChapterUtil.getScanlators(it.scanlator)
            groups.none { group ->
                val inBlocked = group in blockedGroups
                val inFiltered =
                    when (filteredGroupList.isEmpty()) {
                        true -> false
                        false -> filteredGroupList.contains(group)
                    }
                val unsupported = group in MdConstants.UnsupportedOfficialScanlators
                inBlocked || inFiltered || unsupported
            }
        }
    }
}
