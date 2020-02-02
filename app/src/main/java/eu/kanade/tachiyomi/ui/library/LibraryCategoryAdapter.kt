package eu.kanade.tachiyomi.ui.library

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.category.CategoryAdapter
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.removeArticles
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.*


/**
 * Adapter storing a list of manga in a certain category.
 *
 * @param view the fragment containing this adapter.
 */
class LibraryCategoryAdapter(val view: LibraryCategoryView) :
        FlexibleAdapter<LibraryItem>(null, view, true) {

    /**
     * The list of manga in this category.
     */
    private var mangas: List<LibraryItem> = emptyList()

    val onItemReleaseListener: CategoryAdapter.OnItemReleaseListener = view

    /**
     * Sets a list of manga in the adapter.
     *
     * @param list the list to set.
     */
    fun setItems(list: List<LibraryItem>) {
        // A copy of manga always unfiltered.
        mangas = list.toList()

        performFilter()
    }

    /**
     * Returns the position in the adapter for the given manga.
     *
     * @param manga the manga to find.
     */
    fun indexOf(manga: Manga): Int {
        return currentItems.indexOfFirst { it.manga.id == manga.id }
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(mangas)
        }
        else {
            updateDataSet(mangas.filter { it.filter(s) })
        }
        isLongPressDragEnabled = view.canDrag() && s.isNullOrBlank()
    }

    override fun onCreateBubbleText(position: Int):String {
        return if (position < scrollableHeaders.size) {
            "Top"
        } else if (position >= itemCount - scrollableFooters.size) {
            "Bottom"
        } else { // Get and show the first character
            val iFlexible: IFlexible<*>? = getItem(position)
            val preferences:PreferencesHelper by injectLazy()
            when (preferences.librarySortingMode().getOrDefault()) {
                LibrarySort.DRAG_AND_DROP -> {
                    if (preferences.showCategories().getOrDefault()) {
                        val title = (iFlexible as LibraryItem).manga.currentTitle()
                        if (preferences.removeArticles().getOrDefault())
                            title.removeArticles().substring(0, 1).toUpperCase(Locale.US)
                        else title.substring(0, 1).toUpperCase(Locale.US)
                    }
                    else {
                        val db:DatabaseHelper by injectLazy()
                        val category = db.getCategoriesForManga((iFlexible as LibraryItem).manga)
                            .executeAsBlocking().firstOrNull()?.name
                        category?.chop(10) ?: "Default"
                    }
                }
                LibrarySort.LAST_READ -> {
                    val db:DatabaseHelper by injectLazy()
                    val id = (iFlexible as LibraryItem).manga.id ?: return ""
                    val history = db.getHistoryByMangaId(id).executeAsBlocking()
                    val last = history.maxBy { it.last_read }
                    if (last != null)
                        getShortDate(Date(last.last_read))
                    else
                        "N/A"
                }
                LibrarySort.UNREAD -> {
                    val unread = (iFlexible as LibraryItem).manga.unread
                    if (unread > 0)
                        unread.toString()
                    else
                        "Read"
                }
                LibrarySort.LAST_UPDATED -> {
                    val lastUpdate = (iFlexible as LibraryItem).manga.last_update
                    if (lastUpdate > 0)
                        getShortDate(Date(lastUpdate))
                    else
                        "N/A"
                }
                else -> {
                    val title = (iFlexible as LibraryItem).manga.currentTitle()
                    if (preferences.removeArticles().getOrDefault())
                        title.removeArticles().substring(0, 1).toUpperCase(Locale.US)
                    else title.substring(0, 1).toUpperCase(Locale.US)
                }
            }
        }
    }

    private fun getShortDate(date:Date):String {
        val cal = Calendar.getInstance()
        cal.time = Date()

        val yearNow = cal.get(Calendar.YEAR)
        val cal2 = Calendar.getInstance()
        cal2.time = date
        val yearThen = cal2.get(Calendar.YEAR)

        return if (yearNow == yearThen)
            SimpleDateFormat("MMM", Locale.getDefault()).format(date)
        else
            SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
    }

}
