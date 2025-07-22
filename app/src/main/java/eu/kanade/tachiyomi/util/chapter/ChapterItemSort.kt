package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterItemSort(
    val chapterFilter: ChapterItemFilter = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
) {

    fun <T : ChapterItem> getChaptersSorted(
        manga: Manga,
        rawChapters: List<T>,
        andFiltered: Boolean = true,
        filterForReader: Boolean = false,
        currentChapter: T? = null,
    ): List<T> {
        val chapters =
            when {
                filterForReader ->
                    chapterFilter.filterChaptersForReader(rawChapters, manga, currentChapter)
                andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
                else -> rawChapters
            }

        return chapters.sortedWith(sortComparator(manga))
    }

    fun <T : ChapterItem> getNextUnreadChapter(
        manga: Manga,
        rawChapters: List<T>,
        andFiltered: Boolean = true,
    ): T? {
        val chapters =
            when {
                andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
                else -> rawChapters
            }
        return chapters.sortedWith(sortComparator(manga, true)).find {
            !it.chapter.read && !it.chapter.isUnavailable
        }
    }

    fun <T : ChapterItem> sortComparator(manga: Manga, ignoreAsc: Boolean = false): Comparator<T> {
        val sortDescending = !ignoreAsc && manga.sortDescending(mangaDetailsPreferences)
        val sortFunction: (T, T) -> Int =
            when (manga.chapterOrder(mangaDetailsPreferences)) {
                Manga.CHAPTER_SORTING_SOURCE ->
                    when (sortDescending) {
                        true -> { c1, c2 ->
                                c1.chapter.sourceOrder.compareTo(c2.chapter.sourceOrder)
                            }
                        false -> { c1, c2 ->
                                c2.chapter.sourceOrder.compareTo(c1.chapter.sourceOrder)
                            }
                    }
                Manga.CHAPTER_SORTING_SMART ->
                    when (sortDescending) {
                        true -> { c1, c2 -> c1.chapter.smartOrder.compareTo(c2.chapter.smartOrder) }
                        false -> { c1, c2 ->
                                c2.chapter.smartOrder.compareTo(c1.chapter.smartOrder)
                            }
                    }
                Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                    when (sortDescending) {
                        true -> { c1, c2 -> c2.chapter.dateUpload.compareTo(c1.chapter.dateUpload) }
                        false -> { c1, c2 ->
                                c1.chapter.dateUpload.compareTo(c2.chapter.dateUpload)
                            }
                    }
                else -> { c1, c2 -> c1.chapter.sourceOrder.compareTo(c2.chapter.sourceOrder) }
            }
        return Comparator(sortFunction)
    }
}
