package eu.kanade.tachiyomi.ui.library

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.f2prateek.rx.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.catalogue_mat_grid_item.view.*
import uy.kohesive.injekt.injectLazy

class LibraryItem(val manga: LibraryManga, private val libraryAsList: Preference<Boolean>) :
        AbstractFlexibleItem<LibraryHolder>(), IFilterable<String> {

    var downloadCount = -1
    var unreadType = 1

    override fun getLayoutRes(): Int {
        return if (libraryAsList.getOrDefault())
            R.layout.catalogue_list_item
        else
            R.layout.catalogue_mat_grid_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): LibraryHolder {
        val parent = adapter.recyclerView
        return if (parent is AutofitRecyclerView) {
            view.apply {
                val coverHeight = (parent.itemWidth / 3 * 4f).toInt()
                constraint_layout.minHeight = coverHeight
            }
            LibraryMatGridHolder(view, adapter as LibraryCategoryAdapter, parent.itemWidth - 22.dpToPx, parent
                .spanCount)

        } else {
            LibraryListHolder(view, adapter as LibraryCategoryAdapter)
        }
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: LibraryHolder,
                                position: Int,
                                payloads: MutableList<Any?>?) {
        holder.onSetValues(this)
    }

    /**
     * Returns true if this item is draggable.
     */
    override fun isDraggable(): Boolean {
        return true
    }

    /**
     * Filters a manga depending on a query.
     *
     * @param constraint the query to apply.
     * @return true if the manga should be included, false otherwise.
     */
    override fun filter(constraint: String): Boolean {
        val sourceManager by injectLazy<SourceManager>()
        val sourceName = if (manga.source == 0L) "Local" else
            sourceManager.getOrStub(manga.source).name
        return manga.currentTitle().contains(constraint, true) ||
            manga.originalTitle().contains(constraint, true) ||
            (manga.currentAuthor()?.contains(constraint, true) ?: false) ||
            (manga.currentArtist()?.contains(constraint, true) ?: false) ||
            sourceName.contains(constraint, true) ||
            if (constraint.contains(",")) {
                val genres = manga.currentGenres()?.split(", ")
                constraint.split(",").all { containsGenre(it.trim(), genres) }
            }
           else containsGenre(constraint, manga.currentGenres()?.split(", "))
    }

    @SuppressLint("DefaultLocale")
    private fun containsGenre(tag: String, genres: List<String>?): Boolean {
        if (tag.trim().isEmpty()) return true
        return if (tag.startsWith("-"))
            genres?.find {
                it.trim().toLowerCase() == tag.substringAfter("-").toLowerCase()
            }                   == null
        else
            genres?.find {
                it.trim().toLowerCase() == tag.toLowerCase() } != null
    }

    override fun equals(other: Any?): Boolean {
        if (other is LibraryItem) {
            return manga.id == other.manga.id
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }
}
