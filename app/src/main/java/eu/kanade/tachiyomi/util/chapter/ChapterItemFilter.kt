package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.merged.comick.Comick
import eu.kanade.tachiyomi.source.online.merged.komga.Komga
import eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi
import eu.kanade.tachiyomi.source.online.merged.toonily.Toonily
import eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterItemFilter(
    val preferences: PreferencesHelper = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
) {

    /** filters chapters based on the manga values */
    fun <T : ChapterItem> filterChapters(chapters: List<T>, manga: Manga): List<T> {
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
        val unavailableEnabled =
            manga.availableFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_AVAILABLE
        val availableEnabled =
            manga.availableFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_UNAVAILABLE

        // if none of the filters are enabled skip the filtering of them
        TimberKt.d { "Filtering by scanlators and language" }
        val filteredChapters = filterChaptersByScanlatorsAndLanguage(chapters, manga, preferences)
        TimberKt.d { "Filtering by scanlators and language done" }

        return if (
            readEnabled ||
                unreadEnabled ||
                downloadEnabled ||
                notDownloadEnabled ||
                bookmarkEnabled ||
                notBookmarkEnabled ||
                unavailableEnabled ||
                availableEnabled
        ) {
            filteredChapters.filter { chapterItem ->
                val chapter = chapterItem.chapter
                val isDownloaded = downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga)
                val isAvailable = !chapter.isUnavailable || isDownloaded || chapter.isLocalSource()
                return@filter !(readEnabled && !chapter.read ||
                    (unreadEnabled && chapter.read) ||
                    (bookmarkEnabled && !chapter.bookmark) ||
                    (notBookmarkEnabled && chapter.bookmark) ||
                    (downloadEnabled && !isDownloaded) ||
                    (notDownloadEnabled && isDownloaded) ||
                    (unavailableEnabled && !isAvailable) ||
                    (availableEnabled && isAvailable))
            }
        } else {
            filteredChapters
        }
    }

    /** filter chapters for the reader */
    fun <T : ChapterItem> filterChaptersForReader(
        chapters: List<T>,
        manga: Manga,
        selectedChapter: T? = null,
    ): List<T> {
        var filteredChapters = filterChaptersByScanlatorsAndLanguage(chapters, manga, preferences)
        // if neither preference is enabled don't even filter
        if (!readerPreferences.skipRead().get() && !readerPreferences.skipFiltered().get()) {
            return filteredChapters
        }

        if (readerPreferences.skipRead().get()) {
            filteredChapters = filteredChapters.filter { !it.chapter.read }
        }
        if (readerPreferences.skipFiltered().get()) {
            filteredChapters = filterChapters(filteredChapters, manga)
        }
        // add the selected chapter to the list in case it was filtered out
        if (selectedChapter != null) {
            val find = filteredChapters.find { it.chapter.id == selectedChapter.chapter.id }
            if (find == null) {
                val mutableList = filteredChapters.toMutableList()
                mutableList.add(selectedChapter)
                filteredChapters = mutableList.toList()
            }
        }

        return filteredChapters
    }

    /** filters chapters for scanlators */
    private fun <T : ChapterItem> filterChaptersByScanlatorsAndLanguage(
        chapters: List<T>,
        manga: Manga,
        preferences: PreferencesHelper,
    ): List<T> {

        val blockedGroups = preferences.blockedScanlators().get().toSet()
        val chapterScanlatorMatchAll = preferences.chapterScanlatorFilterOption().get() == 0
        val filteredGroups = ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
        val filteredLanguages = ChapterUtil.getLanguages(manga.filtered_language).toSet()
        return chapters
            .asSequence()
            .filterNot { chapterItem ->
                ChapterUtil.filteredBySource(
                    MdConstants.name,
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.isMergedChapter(),
                    filteredGroups,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filteredBySource(
                    Comick.name,
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.isMergedChapter(),
                    filteredGroups,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filteredBySource(
                    Komga.name,
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.isMergedChapter(),
                    filteredGroups,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filteredBySource(
                    Suwayomi.name,
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.isMergedChapter(),
                    filteredGroups,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filteredBySource(
                    Toonily.name,
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.isMergedChapter(),
                    filteredGroups,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filteredBySource(
                    WeebCentral.name,
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.isMergedChapter(),
                    filteredGroups,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filterByLanguage(chapterItem.chapter.language, filteredLanguages)
            }
            // blocked groups are always Any
            .filterNot { chapterItem ->
                ChapterUtil.filterByGroup(chapterItem.chapter.scanlator, false, blockedGroups)
            }
            .filterNot { chapterItem ->
                ChapterUtil.filterByGroup(
                    chapterItem.chapter.scanlator,
                    chapterScanlatorMatchAll,
                    filteredGroups,
                )
            }
            .toList()
    }
}
