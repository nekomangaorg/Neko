package eu.kanade.tachiyomi.ui.library

import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.category.CategoryAdapter
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.removeArticles
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
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
        var s = getFilter(String::class.java)
        if (s == null) {
            s = ""
        }
        isLongPressDragEnabled = view.canDrag() && s.isNullOrBlank()
        updateDataSet(mangas.filter { it.filter(s) })
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
                LibrarySort.LAST_READ -> {
                    val db:DatabaseHelper by injectLazy()
                    val id = (iFlexible as LibraryItem).manga.id ?: return ""
                    val history = db.getHistoryByMangaId(id).executeAsBlocking()
                    if (history.firstOrNull() != null)
                        getShortDate(Date(history.first().last_read))
                    else
                        "Never Read"
                }
                LibrarySort.LAST_UPDATED -> {
                    val lastUpdate = (iFlexible as LibraryItem).manga.last_update
                    if (lastUpdate > 0)
                        getShortDate(Date(lastUpdate))
                    else
                        "N/A"
                }
                else -> {
                    val title = (iFlexible as LibraryItem).manga.title
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
            SimpleDateFormat("MMM").format(date)
        else
            SimpleDateFormat("yyyy").format(date)
    }

}
