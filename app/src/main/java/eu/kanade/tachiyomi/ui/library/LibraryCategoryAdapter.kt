package eu.kanade.tachiyomi.ui.library

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.lang.removeArticles
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Adapter storing a list of manga in a certain category.
 *
 * @param view the fragment containing this adapter.
 */
class LibraryCategoryAdapter(val libraryListener: LibraryListener) :
        FlexibleAdapter<IFlexible<*>>(null, libraryListener, true) {

    init {
        setDisplayHeadersAtStartUp(Injekt.get<PreferencesHelper>().libraryAsSingleList()
            .getOrDefault())
    }
    /**
     * The list of manga in this category.
     */
    private var mangas: List<LibraryItem> = emptyList()

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
    fun indexOf(categoryOrder: Int): Int {
        return currentItems.indexOfFirst {
            if (it is LibraryHeaderItem) it.category.order == categoryOrder
            else false }
    }

    /**
     * Returns the position in the adapter for the given manga.
     *
     * @param manga the manga to find.
     */
    fun indexOf(manga: Manga): Int {
        return currentItems.indexOfFirst {
            if (it is LibraryItem) it.manga.id == manga.id
            else false }
    }

    fun getHeaderPositions(): List<Int> {
        return currentItems.mapIndexedNotNull { index, it ->
            if (it is LibraryHeaderItem) index
            else null }
    }

    /**
     * Returns the position in the adapter for the given manga.
     *
     * @param manga the manga to find.
     */
    fun allIndexOf(manga: Manga): List<Int> {
        return currentItems.mapIndexedNotNull { index, it ->
            if (it is LibraryItem &&  it.manga.id == manga.id) index
            else null }
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(mangas)
        }
        else {
            updateDataSet(mangas.filter { it.filter(s) })
        }
        isLongPressDragEnabled = libraryListener.canDrag() && s.isNullOrBlank()
    }

    override fun onCreateBubbleText(position: Int):String {
        return if (position < scrollableHeaders.size) {
            "Top"
        } else if (position >= itemCount - scrollableFooters.size) {
            "Bottom"
        } else { // Get and show the first character
            val iFlexible: IFlexible<*>? = getItem(position)
            if (iFlexible is LibraryHeaderItem) {
                return iFlexible.category.name
            }
            val preferences:PreferencesHelper by injectLazy()
            when (preferences.librarySortingMode().getOrDefault()) {
                LibrarySort.DRAG_AND_DROP -> {
                    if (!preferences.hideCategories().getOrDefault()) {
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
                LibrarySort.LATEST_CHAPTER -> {
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

    interface LibraryListener {
        /**
         * Called when an item of the list is released.
         */
        fun startReading(position: Int)
        fun onItemReleased(position: Int)
        fun canDrag(): Boolean
        fun updateCategory(catId: Int): Boolean
        fun sortCategory(catId: Int, sortBy: Int)
        fun selectAll(position: Int)
        fun allSelected(position: Int): Boolean
        fun recyclerIsScrolling(): Boolean
    }
}
