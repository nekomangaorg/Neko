package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.domain.chapter.ChapterItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterItemSort(
    val manga: Manga,
    val chapterFilter: ChapterItemFilter = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
) {

    fun <T : ChapterItem> getChaptersSorted(
        rawChapters: List<T>,
        andFiltered: Boolean = true,
        filterForReader: Boolean = false,
        currentChapter: T? = null,
    ): List<T> {
        val chapters = when {
            filterForReader -> chapterFilter.filterChaptersForReader(
                rawChapters,
                manga,
                currentChapter,
            )
            andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
            else -> rawChapters
        }

        return chapters.sortedWith(sortComparator())
    }

    fun <T : ChapterItem> getNextUnreadChapter(
        rawChapters: List<T>,
        andFiltered: Boolean = true,
    ): T? {
        val chapters = when {
            andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
            else -> rawChapters
        }
        return chapters.sortedWith(sortComparator(true)).find { !it.chapter.read }
    }

    fun <T : ChapterItem> sortComparator(ignoreAsc: Boolean = false): Comparator<T> {
        val sortDescending = !ignoreAsc && manga.sortDescending(preferences)
        val sortFunction: (T, T) -> Int =
            when (manga.chapterOrder(preferences)) {
                Manga.CHAPTER_SORTING_SOURCE -> when (sortDescending) {
                    true -> { c1, c2 -> c1.chapter.sourceOrder.compareTo(c2.chapter.sourceOrder) }
                    false -> { c1, c2 -> c2.chapter.sourceOrder.compareTo(c1.chapter.sourceOrder) }
                }
                Manga.CHAPTER_SORTING_NUMBER -> when (sortDescending) {
                    true -> { c1, c2 -> c2.chapter.chapterNumber.compareTo(c1.chapter.chapterNumber) }
                    false -> { c1, c2 -> c1.chapter.chapterNumber.compareTo(c2.chapter.chapterNumber) }
                }
                Manga.CHAPTER_SORTING_UPLOAD_DATE -> when (sortDescending) {
                    true -> { c1, c2 -> c2.chapter.dateUpload.compareTo(c1.chapter.dateUpload) }
                    false -> { c1, c2 -> c1.chapter.dateUpload.compareTo(c2.chapter.dateUpload) }
                }
                else -> { c1, c2 -> c1.chapter.sourceOrder.compareTo(c2.chapter.sourceOrder) }
            }
        return Comparator(sortFunction)
    }
}
