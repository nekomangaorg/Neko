package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.reader.ReaderPreferences
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
        val downloadEnabled = manga.downloadedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_DOWNLOADED
        val notDownloadEnabled =
            manga.downloadedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_NOT_DOWNLOADED
        val bookmarkEnabled = manga.bookmarkedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_BOOKMARKED
        val notBookmarkEnabled =
            manga.bookmarkedFilter(mangaDetailsPreferences) == Manga.CHAPTER_SHOW_NOT_BOOKMARKED

        // if none of the filters are enabled skip the filtering of them
        val filteredChapters = filterChaptersByLanguage(filterChaptersByScanlators(chapters, manga), manga)
        return if (readEnabled || unreadEnabled || downloadEnabled || notDownloadEnabled || bookmarkEnabled || notBookmarkEnabled) {
            filteredChapters.filter { chapterItem ->
                val chapter = chapterItem.chapter
                if (readEnabled && chapter.read.not() ||
                    (unreadEnabled && chapter.read) ||
                    (bookmarkEnabled && chapter.bookmark.not()) ||
                    (notBookmarkEnabled && chapter.bookmark) ||
                    (downloadEnabled && downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga).not()) ||
                    (notDownloadEnabled && downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga))
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
    fun <T : ChapterItem> filterChaptersForReader(
        chapters: List<T>,
        manga: Manga,
        selectedChapter: T? = null,
    ): List<T> {
        var filteredChapters = filterChaptersByScanlators(chapters, manga)
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
    fun <T : ChapterItem> filterChaptersByScanlators(chapters: List<T>, manga: Manga): List<T> {
        return manga.filtered_scanlators?.let { filteredScanlatorString ->
            val filteredScanlators = ChapterUtil.getScanlators(filteredScanlatorString)
            chapters.filter {
                ChapterUtil.getScanlators(it.chapter.scanlator)
                    .none { group -> filteredScanlators.contains(group) }
            }
        } ?: chapters
    }

    /** filters chapters for language */
    fun <T : ChapterItem> filterChaptersByLanguage(chapters: List<T>, manga: Manga): List<T> {
        return manga.filtered_language?.let { filteredLanguageString ->
            val filteredLanguages = ChapterUtil.getLanguages(filteredLanguageString)
            chapters.filter {
                ChapterUtil.getLanguages(it.chapter.language)
                    .none { lang -> filteredLanguages.contains(lang) }
            }
        } ?: chapters
    }
}
