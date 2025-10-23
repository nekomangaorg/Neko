package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterItemSort(
    val chapterFilter: ChapterItemFilter = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get(),
) {

    fun <T : ChapterItem> getChaptersSorted(manga: Manga, rawChapters: List<T>): List<T> {
        return chapterFilter.filterChapters(rawChapters, manga).sortedWith(sortComparator(manga))
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
            }.filter { it.chapter.scanlator !in MdConstants.UnsupportedOfficialGroupList }
        return chapters.sortedWith(sortComparator(manga, true)).find {
            !it.chapter.read && !it.chapter.isUnavailable
        }
    }

    fun <T : ChapterItem> sortComparator(manga: Manga, ignoreAsc: Boolean = false): Comparator<T> {
        val sortDescending = !ignoreAsc && manga.sortDescending(mangaDetailsPreferences)
        val sourceOrderAsc = compareBy<T> { it.chapter.sourceOrder }
        // smart order numbers are flipped so this adjusts them to match the same as the other sort
        // types
        val smartOrderDesc = compareBy<T> { it.chapter.smartOrder }
        val uploadDateAsc = compareBy<T> { it.chapter.dateUpload }

        val sourceOrderDesc = sourceOrderAsc.reversed()
        val smartOrderAsc = smartOrderDesc.reversed()
        val uploadDateDesc = uploadDateAsc.reversed()

        // Build the final comparator
        return when (manga.chapterOrder(mangaDetailsPreferences)) {
            Manga.CHAPTER_SORTING_SOURCE -> if (sortDescending) sourceOrderAsc else sourceOrderDesc

            Manga.CHAPTER_SORTING_SMART -> if (sortDescending) smartOrderAsc else smartOrderDesc

            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                if (sortDescending) {
                    uploadDateDesc.thenComparing(smartOrderAsc)
                } else {
                    uploadDateAsc.thenComparing(smartOrderDesc)
                }

            else -> if (sortDescending) sourceOrderAsc else sourceOrderDesc
        }
    }
}
