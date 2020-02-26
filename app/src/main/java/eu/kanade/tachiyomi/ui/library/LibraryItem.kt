package eu.kanade.tachiyomi.ui.library

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.f2prateek.rx.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.catalogue_grid_item.view.*
import uy.kohesive.injekt.injectLazy

class LibraryItem(val manga: LibraryManga,
    private val libraryLayout: Preference<Int>,
    header: LibraryHeaderItem?) :
    AbstractSectionableItem<LibraryHolder, LibraryHeaderItem?>(header), IFilterable<String> {

    var downloadCount = -1
    var unreadType = 1
    var chapterCount = -1

    override fun getLayoutRes(): Int {
        return if (libraryLayout.getOrDefault() == 0)
            R.layout.catalogue_list_item
        else
            R.layout.catalogue_grid_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): LibraryHolder {
        val parent = adapter.recyclerView
        return if (parent is AutofitRecyclerView) {
            val libraryLayout = libraryLayout.getOrDefault()
            if (libraryLayout == 0) {
                LibraryListHolder(view, adapter as LibraryCategoryAdapter)
            }
            else {
                view.apply {
                    val coverHeight = (parent.itemWidth / 3f * 4f).toInt()
                    if (libraryLayout == 1) {
                        constraint_layout.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        val marginParams = card.layoutParams as ConstraintLayout.LayoutParams
                        marginParams.bottomMargin = 6.dpToPx
                        card.layoutParams = marginParams
                        cover_thumbnail.maxHeight = coverHeight
                        constraint_layout.minHeight = 0
                        cover_thumbnail.adjustViewBounds = false
                        cover_thumbnail.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            coverHeight
                        )
                    } else if (libraryLayout == 2) {
                        constraint_layout.minHeight = coverHeight
                        cover_thumbnail.minimumHeight = (parent.itemWidth / 3f * 3.6f).toInt()
                        cover_thumbnail.maxHeight = (parent.itemWidth / 3f * 6f).toInt()
                        constraint_layout.background = ContextCompat.getDrawable(
                            context, R.drawable.library_item_selector
                        )
                    }
                }
                LibraryGridHolder(
                    view,
                    adapter as LibraryCategoryAdapter,
                    parent.itemWidth,
                    libraryLayout == 1
                )
            }
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
            return manga.id == other.manga.id && manga.category == other.manga.category
        }
        return false
    }

    override fun hashCode(): Int {
        return (manga.id!! + (manga.category shl 50).toLong()).hashCode()  //!!.hashCode()
    }
}
