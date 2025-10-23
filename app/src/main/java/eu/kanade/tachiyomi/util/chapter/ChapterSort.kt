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

    fun <T : Chapter> sortComparator(ignoreAsc: Boolean = false): Comparator<T> {
        val sortDescending = !ignoreAsc && manga.sortDescending(mangaDetailsPreferences)
        val sourceOrderAsc = compareBy<T> { it.source_order }
        // smart order numbers are flipped so this adjusts them to match the same as the other sort
        // types
        val smartOrderDesc = compareBy<T> { it.smart_order }
        val uploadDateAsc = compareBy<T> { it.date_upload }

        val sourceOrderDesc = sourceOrderAsc.reversed()
        val smartOrderAsc = smartOrderDesc.reversed()
        val uploadDateDesc = uploadDateAsc.reversed()

        // Build the final comparator
        return when (manga.chapterOrder(mangaDetailsPreferences)) {
            Manga.CHAPTER_SORTING_SOURCE -> if (sortDescending) sourceOrderAsc else sourceOrderDesc

            Manga.CHAPTER_SORTING_SMART -> if (sortDescending) smartOrderAsc else smartOrderDesc

            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                if (sortDescending) {
                    uploadDateDesc.thenComparing(smartOrderDesc)
                } else {
                    uploadDateAsc.thenComparing(sourceOrderAsc)
                }

            else -> if (sortDescending) sourceOrderAsc else sourceOrderDesc
        }
    }
}
