package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterSort(
    val manga: Manga,
    val chapterFilter: ChapterFilter = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
) {

    fun <T : Chapter> getChaptersSorted(
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

        return chapters.sortedWith(sortComparator())
    }

    fun <T : Chapter> getNextUnreadChapter(rawChapters: List<T>, andFiltered: Boolean = true): T? {
        val chapters =
            when {
                andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
                else -> rawChapters
            }
        return chapters.sortedWith(sortComparator(true)).find { !it.read }
    }

    fun <T : Chapter> sortComparator(ignoreAsc: Boolean = false): Comparator<T> {
        val sortDescending = !ignoreAsc && manga.sortDescending(mangaDetailsPreferences)
        val sortFunction: (T, T) -> Int =
            when (manga.chapterOrder(mangaDetailsPreferences)) {
                Manga.CHAPTER_SORTING_SOURCE ->
                    when (sortDescending) {
                        true -> { c1, c2 -> c1.source_order.compareTo(c2.source_order) }
                        false -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
                    }
                Manga.CHAPTER_SORTING_SMART ->
                    when (sortDescending) {
                        true -> { c1, c2 -> c2.smart_order.compareTo(c1.smart_order) }
                        false -> { c1, c2 -> c1.smart_order.compareTo(c2.smart_order) }
                    }
                Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                    when (sortDescending) {
                        true -> { c1, c2 -> c2.date_upload.compareTo(c1.date_upload) }
                        false -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
                    }
                else -> { c1, c2 -> c1.source_order.compareTo(c2.source_order) }
            }
        return Comparator(sortFunction)
    }
}
