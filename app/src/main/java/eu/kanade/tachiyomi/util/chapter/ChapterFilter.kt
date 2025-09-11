package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isLocalSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.isAvailable
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterFilter(
    val preferences: PreferencesHelper = Injekt.get(),
    val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
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
        val availableEnabled =
            manga.availableFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_AVAILABLE
        val unavailableEnabled =
            manga.availableFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_UNAVAILABLE

        // if none of the filters are enabled skip the filtering of them
        val filteredChapters =
            filterChaptersByScanlatorsAndLanguage(
                chapters,
                manga,
                mangaDexPreferences,
                libraryPreferences,
            )

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
            filteredChapters.filter {
                val isDownloaded = downloadManager.isChapterDownloaded(it, manga)
                val isAvailable = it.isAvailable(isDownloaded)
                return@filter !(readEnabled && !it.read ||
                    (unreadEnabled && it.read) ||
                    (bookmarkEnabled && !it.bookmark) ||
                    (notBookmarkEnabled && it.bookmark) ||
                    (downloadEnabled && !isDownloaded) ||
                    (notDownloadEnabled && isDownloaded) ||
                    (unavailableEnabled && isAvailable) ||
                    (availableEnabled && !isAvailable))
            }
        } else {
            filteredChapters
        }
    }

    /** filter chapters for the reader */
    fun <T : Chapter> filterChaptersForReader(
        chapters: List<T>,
        manga: Manga,
        comparator: Comparator<T>,
        selectedChapter: T? = null,
    ): List<T> {
        var filteredChapters = chapters.filter { it.isAvailable(downloadManager, manga) }
        filteredChapters =
            filterChaptersByScanlatorsAndLanguage(
                filteredChapters,
                manga,
                mangaDexPreferences,
                libraryPreferences,
            )
        filteredChapters =
            filteredChapters.filter { it.scanlator !in MdConstants.UnsupportedOfficialGroupList }

        // if filter preferences are not enabled don't even filter
        if (
            !readerPreferences.skipRead().get() &&
                !readerPreferences.skipFiltered().get() &&
                !readerPreferences.skipDuplicates().get()
        ) {
            return filteredChapters.sortedWith(comparator)
        }

        if (readerPreferences.skipRead().get()) {
            filteredChapters = filteredChapters.filter { !it.read }
        }
        if (readerPreferences.skipFiltered().get()) {
            filteredChapters = filterChapters(filteredChapters, manga)
        }

        filteredChapters = filteredChapters.sortedWith(comparator)

        if (readerPreferences.skipDuplicates().get()) {
            filteredChapters =
                filteredChapters.partitionByChapterNumber().map { chapters ->
                    chapters.find { it.id == selectedChapter?.id }
                        ?: chapters.find {
                            it.scanlator == selectedChapter?.scanlator &&
                                it.uploader == selectedChapter?.uploader
                        }
                        ?: chapters.maxBy {
                            val mainScans = ChapterUtil.getScanlators(it.scanlator).toMutableSet()
                            if (Constants.NO_GROUP in mainScans)
                                it.uploader?.let { up -> mainScans.add(up) }
                            val currScans =
                                ChapterUtil.getScanlators(selectedChapter?.scanlator).toMutableSet()
                            if (Constants.NO_GROUP in currScans)
                                selectedChapter!!.uploader?.let { up -> currScans.add(up) }
                            mainScans.intersect(currScans).size
                        }
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

    // Adapted from https://stackoverflow.com/a/65465410
    fun <T : Chapter> List<T>.partitionByChapterNumber(): List<List<T>> {
        val result = mutableListOf<List<T>>()
        var currentList = mutableListOf<T>()
        this.forEachIndexed { i, ch ->
            currentList.add(ch)
            if (
                i + 1 >= this.size ||
                    getChapterNum(ch) == null ||
                    getChapterNum(ch) != getChapterNum(this[i + 1]) ||
                    (getVolumeNum(ch) != getVolumeNum(this[i + 1]) &&
                        getVolumeNum(ch) != null &&
                        getVolumeNum(this[i + 1]) != null)
            ) {
                result.add(currentList.toList())
                currentList = mutableListOf()
            }
        }
        return result
    }

    /**
     * filters chapters for scanlators, excludes globally blocked, unsupported and manga specific
     * filtered
     */
    /** filters chapters for scanlators */
    fun <T : Chapter> filterChaptersByScanlatorsAndLanguage(
        chapters: List<T>,
        manga: Manga,
        mangaDexPreferences: MangaDexPreferences,
        libraryPreferences: LibraryPreferences,
    ): List<T> {

        val blockedGroups = mangaDexPreferences.blockedGroups().get().toSet()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get().toSet()

        // Filtered sources, groups and uploaders
        val filtered = ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
        val filteredLanguages = ChapterUtil.getLanguages(manga.filtered_language).toSet()

        val sources = SourceManager.mergeSourceNames + MdConstants.name
        val scanlatorMatchAll = libraryPreferences.chapterScanlatorFilterOption().get() == 0

        return chapters
            .asSequence()
            .filterNot { chapter ->
                sources.any { sourceName ->
                    ChapterUtil.filteredBySource(
                        sourceName,
                        chapter.scanlator ?: "",
                        chapter.isMergedChapter(),
                        chapter.isLocalSource(),
                        filtered,
                    )
                }
            }
            .filterNot { chapter ->
                ChapterUtil.filterByLanguage(chapter.language ?: "", filteredLanguages)
            }
            // blocked groups are always Any
            .filterNot { chapter ->
                ChapterUtil.filterByScanlator(
                    chapter.scanlator ?: "",
                    chapter.uploader ?: "",
                    false,
                    blockedGroups,
                    blockedUploaders,
                )
            }
            .filterNot { chapter ->
                ChapterUtil.filterByScanlator(
                    chapter.scanlator ?: "",
                    chapter.uploader ?: "",
                    scanlatorMatchAll,
                    filtered,
                )
            }
            .toList()
    }
}
