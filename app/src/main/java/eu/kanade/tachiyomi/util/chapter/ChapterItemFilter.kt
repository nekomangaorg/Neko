package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.isAvailable
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterItemFilter(
    val preferences: PreferencesHelper = Injekt.get(),
    val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
) {

    /** filters chapters based on the manga values */
    fun filterChapters(chapters: List<ChapterItem>, manga: Manga): List<ChapterItem> {
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
        TimberKt.d { "Filtering by scanlators and language" }
        val filteredChapters =
            filterChaptersByScanlatorsAndLanguage(
                chapters,
                manga,
                mangaDexPreferences,
                libraryPreferences,
            )
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
                val chapter = chapterItem.chapter.toDbChapter()
                val isDownloaded = downloadManager.isChapterDownloaded(chapter, manga)
                val isAvailable = chapter.isAvailable(isDownloaded)
                return@filter !(readEnabled && !chapter.read ||
                    (unreadEnabled && chapter.read) ||
                    (bookmarkEnabled && !chapter.bookmark) ||
                    (notBookmarkEnabled && chapter.bookmark) ||
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
    fun filterChaptersForReader(
        chapters: List<ChapterItem>,
        manga: Manga,
        comparator: Comparator<ChapterItem>,
        selectedChapterItem: ChapterItem? = null,
    ): List<ChapterItem> {
        // 1. Filter by isAvailable
        var filteredChapters =
            chapters.filter {
                val dbChapter = it.chapter.toDbChapter()
                val isDownloaded = downloadManager.isChapterDownloaded(dbChapter, manga)
                // Use the extension function for Chapter
                dbChapter.isAvailable(isDownloaded)
            }

        // 2. Filter by scanlators/language
        filteredChapters =
            filterChaptersByScanlatorsAndLanguage(
                filteredChapters,
                manga,
                mangaDexPreferences,
                libraryPreferences,
            )

        // 3. Filter out unsupported groups
        filteredChapters =
            filteredChapters.filter {
                it.chapter.scanlator !in MdConstants.UnsupportedOfficialGroupList
            }

        // 4. Check if reader filters are enabled
        if (
            !readerPreferences.skipRead().get() &&
                !readerPreferences.skipFiltered().get() &&
                !readerPreferences.skipDuplicates().get()
        ) {
            return filteredChapters.sortedWith(comparator)
        }

        // 5. Apply skipRead
        if (readerPreferences.skipRead().get()) {
            filteredChapters = filteredChapters.filter { !it.chapter.read }
        }

        // 6. Apply skipFiltered (use the *other* filter method)
        if (readerPreferences.skipFiltered().get()) {
            filteredChapters = filterChapters(filteredChapters, manga)
        }

        // 7. Sort
        filteredChapters = filteredChapters.sortedWith(comparator)

        // 8. Apply skipDuplicates
        if (readerPreferences.skipDuplicates().get()) {
            filteredChapters =
                filteredChapters.partitionByChapterNumber().mapNotNull { chapterItems ->
                    chapterItems.find { it.chapter.id == selectedChapterItem?.chapter?.id }
                        ?: chapterItems.find {
                            it.chapter.scanlator == selectedChapterItem?.chapter?.scanlator &&
                                it.chapter.uploader == selectedChapterItem?.chapter?.uploader
                        }
                        ?: chapterItems.maxByOrNull {
                            val mainScans =
                                ChapterUtil.getScanlators(it.chapter.scanlator).toMutableSet()
                            if (Constants.NO_GROUP in mainScans)
                                it.chapter.uploader?.let { up -> mainScans.add(up) }

                            val currScans =
                                ChapterUtil.getScanlators(selectedChapterItem?.chapter?.scanlator)
                                    .toMutableSet()
                            if (Constants.NO_GROUP in currScans)
                                selectedChapterItem?.chapter?.uploader?.let { up ->
                                    currScans.add(up)
                                }

                            mainScans.intersect(currScans).size
                        }
                }
        }

        // 9. Add selected chapter back if filtered
        if (selectedChapterItem?.chapter?.id != null) {
            val find = filteredChapters.find { it.chapter.id == selectedChapterItem.chapter.id }
            if (find == null) {
                val mutableList = filteredChapters.toMutableList()
                mutableList.add(selectedChapterItem)
                // Re-sort after adding
                filteredChapters = mutableList.sortedWith(comparator)
            }
        }

        return filteredChapters
    }

    /** filters chapters for scanlators */
    fun filterChaptersByScanlatorsAndLanguage(
        chapters: List<ChapterItem>,
        manga: Manga,
        mangaDexPreferences: MangaDexPreferences,
        libraryPreferences: LibraryPreferences,
    ): List<ChapterItem> {

        val blockedGroups = mangaDexPreferences.blockedGroups().get().toSet()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get().toSet()

        // Filtered sources, groups and uploaders
        val filteredScanlators = ChapterUtil.getScanlators(manga.filtered_scanlators).toSet()
        val filteredLanguages = ChapterUtil.getLanguages(manga.filtered_language).toSet()

        val sources = SourceManager.mergeSourceNames + MdConstants.name
        val scanlatorMatchAll = libraryPreferences.chapterScanlatorFilterOption().get() == 0

        return chapters.filterNot { chapterItem ->
            val scanlators = ChapterUtil.getScanlators(chapterItem.chapter.scanlator)
            val languages = ChapterUtil.getLanguages(chapterItem.chapter.language)

            val sourceFiltered =
                sources.any { sourceName ->
                    ChapterUtil.filteredBySource(
                        sourceName,
                        scanlators,
                        chapterItem.chapter.isMergedChapter(),
                        chapterItem.chapter.isLocalSource(),
                        filteredScanlators,
                    )
                }

            val languageFiltered = ChapterUtil.filterByLanguage(languages, filteredLanguages)

            val blockedScanlator =
                ChapterUtil.filterByScanlator(
                    scanlators,
                    chapterItem.chapter.uploader,
                    false,
                    blockedGroups,
                    blockedUploaders,
                )
            val filteredScanlator =
                ChapterUtil.filterByScanlator(
                    scanlators,
                    chapterItem.chapter.uploader,
                    scanlatorMatchAll,
                    filteredScanlators,
                )
            sourceFiltered || languageFiltered || blockedScanlator || filteredScanlator
        }
    }

    // Adapted from https://stackoverflow.com/a/65465410
    fun List<ChapterItem>.partitionByChapterNumber(): List<List<ChapterItem>> {
        val result = mutableListOf<List<ChapterItem>>()
        var currentList = mutableListOf<ChapterItem>()
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
}
