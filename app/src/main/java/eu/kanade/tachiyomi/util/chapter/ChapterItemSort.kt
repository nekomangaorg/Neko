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

    fun getChaptersSorted(manga: Manga, rawChapters: List<ChapterItem>): List<ChapterItem> {
        return chapterFilter.filterChapters(rawChapters, manga).sortedWith(sortComparator(manga))
    }

    fun getNextUnreadChapter(
        manga: Manga,
        rawChapters: List<ChapterItem>,
        andFiltered: Boolean = true,
    ): ChapterItem? {
        val chapters =
            when {
                andFiltered -> chapterFilter.filterChapters(rawChapters, manga)
                else -> rawChapters
            }.filter { it.chapter.scanlator !in MdConstants.UnsupportedOfficialGroupList }
        return chapters.sortedWith(sortComparator(manga, true)).find {
            !it.chapter.read && !it.chapter.isUnavailable
        }
    }

    fun sortComparator(manga: Manga, forceAscending: Boolean = false): Comparator<ChapterItem> {
        // arrow down in UI is ASC.  The below sorts them all that way.
        val sortAsc =
            if (forceAscending) {
                true
            } else {
                manga.sortDescending(mangaDetailsPreferences) // this really is Asc
            }

        return when (manga.chapterOrder(mangaDetailsPreferences)) {
            Manga.CHAPTER_SORTING_SOURCE ->
                when (sortAsc) {
                    true -> compareBy { it.chapter.sourceOrder }
                    false -> compareByDescending { it.chapter.sourceOrder }
                }

            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                when (sortAsc) {
                    true ->
                        compareBy<ChapterItem> { it.chapter.dateUpload }
                            .thenByDescending { it.chapter.smartOrder }
                    false ->
                        compareByDescending<ChapterItem> { it.chapter.dateUpload }
                            .thenBy { it.chapter.smartOrder }
                }
            else -> { // default is Smart Sort
                when (sortAsc) {
                    true -> compareByDescending { it.chapter.smartOrder }
                    false -> compareBy { it.chapter.smartOrder }
                }
            }
        }
    }
}
