package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Deprecated("Remove once the Reader is using ChapterItem")
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
        return when {
            filterForReader ->
                chapterFilter.filterChaptersForReader(
                    rawChapters,
                    manga,
                    sortComparator(),
                    currentChapter,
                )
            andFiltered ->
                chapterFilter.filterChapters(rawChapters, manga).sortedWith(sortComparator())
            else -> rawChapters.sortedWith(sortComparator())
        }
    }

    fun <T : Chapter> getNextUnreadChapter(rawChapters: List<T>, andFiltered: Boolean = true): T? {
        val chapters =
            when {
                andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
                else -> rawChapters
            }.filter { it.scanlator !in MdConstants.UnsupportedOfficialGroupList }
        return chapters.sortedWith(sortComparator(true)).find { !it.read }
    }

    fun <T : Chapter> sortComparator(forceAscending: Boolean = false): Comparator<T> {

        val sortAsc =
            if (forceAscending) {
                true
            } else {
                manga.sortDescending(mangaDetailsPreferences) // this really is Asc
            }
        return when (manga.chapterOrder(mangaDetailsPreferences)) {
            Manga.CHAPTER_SORTING_SOURCE ->
                when (sortAsc) {
                    true -> compareBy { it.source_order }
                    false -> compareByDescending { it.source_order }
                }
            Manga.CHAPTER_SORTING_SMART ->
                when (sortAsc) {
                    true -> compareByDescending { it.smart_order }
                    false -> compareBy { it.smart_order }
                }
            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                when (sortAsc) {
                    true -> compareBy<T> { it.date_upload }.thenByDescending { it.smart_order }
                    false -> compareByDescending<T> { it.date_upload }.thenBy { it.smart_order }
                }
            else -> {
                compareBy { it.source_order }
            }
        }
    }
}
