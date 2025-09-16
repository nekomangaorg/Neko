package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.isAvailable
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

    /** filters chapters for scanlators */
    private fun <T : ChapterItem> filterChaptersByScanlatorsAndLanguage(
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
            .filterNot { chapterItem ->
                sources.any { sourceName ->
                    ChapterUtil.filteredBySource(
                        sourceName,
                        chapterItem.chapter.scanlator,
                        chapterItem.chapter.isMergedChapter(),
                        chapterItem.chapter.isLocalSource(),
                        filtered,
                    )
                }
            }
            .filterNot { chapterItem ->
                ChapterUtil.filterByLanguage(chapterItem.chapter.language, filteredLanguages)
            }
            // blocked groups are always Any
            .filterNot { chapterItem ->
                ChapterUtil.filterByScanlator(
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.uploader,
                    false,
                    blockedGroups,
                    blockedUploaders,
                )
            }
            .filterNot { chapterItem ->
                ChapterUtil.filterByScanlator(
                    chapterItem.chapter.scanlator,
                    chapterItem.chapter.uploader,
                    scanlatorMatchAll,
                    filtered,
                )
            }
            .toList()
    }
}
