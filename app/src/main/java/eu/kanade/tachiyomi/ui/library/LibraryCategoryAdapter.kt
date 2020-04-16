package eu.kanade.tachiyomi.ui.library

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.lang.removeArticles
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Adapter storing a list of manga in a certain category.
 *
 * @param view the fragment containing this adapter.
 */
class LibraryCategoryAdapter(val libraryListener: LibraryListener) :
    FlexibleAdapter<IFlexible<*>>(null, libraryListener, true) {

    init {
        setDisplayHeadersAtStartUp(true)
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
            if (it is LibraryItem && it.manga.id == manga.id) index
            else null }
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(mangas)
        } else {
            updateDataSet(mangas.filter { it.filter(s) })
        }
        isLongPressDragEnabled = libraryListener.canDrag() && s.isNullOrBlank()
    }

    fun getSectionText(position: Int): String? {
        val preferences: PreferencesHelper by injectLazy()
        val db: DatabaseHelper by injectLazy()
        if (position == itemCount - 1) return "-"
        val sorting = if (preferences.hideCategories().getOrDefault())
            preferences.hideCategories().getOrDefault()
        else (headerItems.firstOrNull() as? LibraryHeaderItem)?.category?.sortingMode()
        ?: LibrarySort.DRAG_AND_DROP
        return when (val item: IFlexible<*>? = getItem(position)) {
            is LibraryHeaderItem ->
                if (preferences.hideCategories().getOrDefault() || item.category.id == 0) null
                else item.category.name.first().toString() +
                    "\u200B".repeat(max(0, item.category.order))
            is LibraryItem -> {
                when (sorting) {
                    LibrarySort.DRAG_AND_DROP -> {
                        val category = db.getCategoriesForManga(item.manga).executeAsBlocking()
                            .firstOrNull()
                        if (category == null) null
                        else getFirstLetter(category.name) + "\u200B".repeat(max(0, category.order))
                    }
                    LibrarySort.LAST_READ -> {
                        val id = item.manga.id ?: return ""
                        val history = db.getHistoryByMangaId(id).executeAsBlocking()
                        val last = history.maxBy { it.last_read }
                        if (last != null && last.last_read > 100) getShorterDate(Date(last.last_read))
                        else "*"
                    }
                    LibrarySort.TOTAL -> {
                        val unread = item.chapterCount
                        (unread / 100).toString()
                    }
                    LibrarySort.UNREAD -> {
                        val unread = item.manga.unread
                        if (unread > 0) (unread / 100).toString()
                        else "R"
                    }
                    LibrarySort.LATEST_CHAPTER -> {
                        val lastUpdate = item.manga.last_update
                        if (lastUpdate > 0) getShorterDate(Date(lastUpdate))
                        else "*"
                    }
                    LibrarySort.DATE_ADDED -> {
                        val lastUpdate = item.manga.date_added
                        if (lastUpdate > 0) getShorterDate(Date(lastUpdate))
                        else "*"
                    }
                    else -> {
                        val title = if (preferences.removeArticles()
                                .getOrDefault()
                        ) item.manga.title.removeArticles()
                        else item.manga.title
                        getFirstLetter(title)
                    }
                }
            }
            else -> ""
        }
    }

    private fun getFirstLetter(name: String): String {
        val letter = name.firstOrNull() ?: '#'
        return if (letter.isLetter()) letter.toString()
            .toUpperCase(Locale.ROOT) else "#"
    }

    override fun onCreateBubbleText(position: Int): String {
        val preferences: PreferencesHelper by injectLazy()
        val db: DatabaseHelper by injectLazy()
        if (position == itemCount - 1) return recyclerView.context.getString(R.string.bottom)
        return when (val iFlexible: IFlexible<*>? = getItem(position)) {
            is LibraryHeaderItem ->
                if (!preferences.hideCategories().getOrDefault()) iFlexible.category.name
                else recyclerView.context.getString(R.string.top)
            is LibraryItem -> {
                if (iFlexible.manga.isBlank()) ""
                else when (preferences.librarySortingMode().getOrDefault()) {
                    LibrarySort.DRAG_AND_DROP -> {
                        if (!preferences.hideCategories().getOrDefault()) {
                            val title = iFlexible.manga.title
                            if (preferences.removeArticles().getOrDefault()) title.removeArticles()
                                .substring(0, 1).toUpperCase(Locale.US)
                            else title.substring(0, 1).toUpperCase(Locale.US)
                        } else {
                            val category = db.getCategoriesForManga(iFlexible.manga)
                                .executeAsBlocking().firstOrNull()?.name
                            category ?: recyclerView.context.getString(R.string.default_value)
                        }
                    }
                    LibrarySort.LAST_READ -> {
                        val id = iFlexible.manga.id ?: return ""
                        val history = db.getHistoryByMangaId(id).executeAsBlocking()
                        val last = history.maxBy { it.last_read }
                        if (last != null && last.last_read > 100) getShortDate(Date(last.last_read))
                        else "N/A"
                    }
                    LibrarySort.UNREAD -> {
                        val unread = iFlexible.manga.unread
                        if (unread > 0) getRange(unread)
                        else recyclerView.context.getString(R.string.read)
                    }
                    LibrarySort.TOTAL -> {
                        val total = iFlexible.chapterCount
                        if (total > 0) getRange(total)
                        else "N/A"
                    }
                    LibrarySort.LATEST_CHAPTER -> {
                        val lastUpdate = iFlexible.manga.last_update
                        if (lastUpdate > 0) getShortDate(Date(lastUpdate))
                        else "N/A"
                    }
                    LibrarySort.DATE_ADDED -> {
                        val lastUpdate = iFlexible.manga.date_added
                        if (lastUpdate > 0) getShortDate(Date(lastUpdate))
                        else "N/A"
                    }
                    else -> getSectionText(position) ?: ""
                }
            }
            else -> ""
        }
    }

    private fun getRange(value: Int): String {
        return when (value) {
            in 1..99 -> "< 100"
            in 100..199 -> "100-199"
            in 200..299 -> "200-299"
            in 300..399 -> "300-399"
            in 400..499 -> "400-499"
            in 500..599 -> "500-599"
            in 600..699 -> "600-699"
            in 700..799 -> "700-799"
            in 800..899 -> "800-899"
            in 900..Int.MAX_VALUE -> "900+"
            else -> "None"
        }
    }

    private fun getShorterDate(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = Date()

        val yearNow = cal.get(Calendar.YEAR)
        val cal2 = Calendar.getInstance()
        cal2.time = date
        val yearThen = cal2.get(Calendar.YEAR)

        return if (yearNow == yearThen)
            SimpleDateFormat("M", Locale.getDefault()).format(date)
        else
            SimpleDateFormat("''yy", Locale.getDefault()).format(date)
    }

    private fun getShortDate(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = Date()

        val yearNow = cal.get(Calendar.YEAR)
        val cal2 = Calendar.getInstance()
        cal2.time = date
        val yearThen = cal2.get(Calendar.YEAR)

        return if (yearNow == yearThen)
            SimpleDateFormat("MMMM", Locale.getDefault()).format(date)
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
        fun toggleCategoryVisibility(position: Int)
    }
}
