package eu.kanade.tachiyomi.ui.library

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.Manga
import java.util.*


/**
 * Adapter storing a list of manga in a certain category.
 *
 * @param view the fragment containing this adapter.
 */
class LibraryCategoryAdapter(view: LibraryCategoryView) :
        FlexibleAdapter<LibraryItem>(null, view, true) {

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
    fun indexOf(manga: Manga): Int {
        return currentItems.indexOfFirst { it.manga.id == manga.id }
    }

    fun performFilter() {
        var s = getFilter(String::class.java)
        if (s == null) {
            s = ""
        }
        updateDataSet(mangas.filter { it.filter(s) })
        
    }

    override fun onCreateBubbleText(position: Int): String {
        return if (position < scrollableHeaders.size) {
            "Top"
        } else if (position >= itemCount - scrollableFooters.size) {
            "Bottom"
        } else { // Get and show the first character
            val iFlexible: IFlexible<*>? = getItem(position)
            (iFlexible as LibraryItem).manga.title.substring(0, 1).toUpperCase(Locale.US)
        }
    }

}
