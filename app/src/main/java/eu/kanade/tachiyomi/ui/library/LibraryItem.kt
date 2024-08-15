package eu.kanade.tachiyomi.ui.library

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFilterable
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.spToPx
import eu.kanade.tachiyomi.util.view.compatToolTipText
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import org.nekomanga.R
import org.nekomanga.databinding.MangaGridItemBinding
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryItem(
    val manga: LibraryManga,
    header: LibraryHeaderItem,
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) : AbstractSectionableItem<LibraryHolder, LibraryHeaderItem>(header), IFilterable<String> {

    var downloadCount = -1
    var unreadType = 2
    var filter = ""

    private val uniformSize: Boolean
        get() = libraryPreferences.uniformGrid().get()

    private val libraryLayout: Int
        get() = libraryPreferences.layout().get()

    val hideReadingButton: Boolean
        get() = libraryPreferences.hideStartReadingButton().get()

    override fun getLayoutRes(): Int {
        return if (libraryLayout == LAYOUT_LIST || manga.isBlank()) {
            R.layout.manga_list_item
        } else {
            R.layout.manga_grid_item
        }
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): LibraryHolder {
        val parent = adapter.recyclerView
        return if (parent is AutofitRecyclerView) {
            val libraryLayout = libraryLayout
            val isFixedSize = uniformSize
            if (libraryLayout == LAYOUT_LIST || manga.isBlank()) {
                LibraryListHolder(view, adapter as LibraryCategoryAdapter)
            } else {
                view.apply {
                    val isStaggered = parent.layoutManager is StaggeredGridLayoutManager
                    val binding = MangaGridItemBinding.bind(this)
                    binding.behindTitle.isVisible = libraryLayout == LAYOUT_COVER_ONLY_GRID
                    if (libraryLayout >= LAYOUT_COMFORTABLE_GRID) {
                        binding.textLayout.isVisible = libraryLayout == LAYOUT_COMFORTABLE_GRID
                        binding.card.setCardForegroundColor(
                            ContextCompat.getColorStateList(
                                context,
                                R.color.library_comfortable_grid_foreground,
                            ),
                        )
                    }
                    if (isFixedSize) {
                        binding.constraintLayout.layoutParams =
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        binding.coverThumbnail.maxHeight = Int.MAX_VALUE
                        binding.coverThumbnail.minimumHeight = 0
                        binding.constraintLayout.minHeight = 0
                        binding.coverThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                        binding.coverThumbnail.adjustViewBounds = false
                        binding.coverThumbnail.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            dimensionRatio = "15:22"
                        }
                    }
                    if (libraryLayout != LAYOUT_COMFORTABLE_GRID) {
                        binding.card.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            bottomMargin = (if (isStaggered) 2 else 6).dpToPx
                        }
                    } else {
                        binding.textLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            height =
                                if (isStaggered) ViewGroup.LayoutParams.WRAP_CONTENT else 31.spToPx
                        }
                    }
                    binding.setBGAndFG(libraryLayout)
                }
                val gridHolder =
                    LibraryGridHolder(
                        view,
                        adapter as LibraryCategoryAdapter,
                        libraryLayout == LAYOUT_COMPACT_GRID,
                        isFixedSize,
                    )
                if (!isFixedSize) {
                    gridHolder.setFreeformCoverRatio(manga, parent)
                }
                gridHolder
            }
        } else {
            LibraryListHolder(view, adapter as LibraryCategoryAdapter)
        }
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: LibraryHolder,
        position: Int,
        payloads: MutableList<Any?>?,
    ) {
        if (holder is LibraryGridHolder && !holder.fixedSize) {
            holder.setFreeformCoverRatio(manga, adapter.recyclerView as? AutofitRecyclerView)
        }
        holder.onSetValues(this)
        (holder as? LibraryGridHolder)?.setSelected(adapter.isSelected(position))
        val layoutParams = holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        layoutParams?.isFullSpan = manga.isBlank()
        if (libraryLayout == LAYOUT_COVER_ONLY_GRID) {
            holder.itemView.compatToolTipText = manga.title
        }
    }

    /** Returns true if this item is draggable. */
    override fun isDraggable(): Boolean {
        return !manga.isBlank() && header.category.isDragAndDrop
    }

    override fun isEnabled(): Boolean {
        return !manga.isBlank()
    }

    override fun isSelectable(): Boolean {
        return !manga.isBlank()
    }

    /**
     * Filters a manga depending on a query.
     *
     * @param constraint the query to apply.
     * @return true if the manga should be included, false otherwise.
     */
    override fun filter(constraint: String): Boolean {
        filter = constraint
        if (manga.isBlank() && manga.title.isBlank()) {
            return constraint.isEmpty()
        }
        return manga.title.contains(constraint, true) ||
            (manga.alt_titles?.contains(constraint, true) ?: false) ||
            (manga.author?.contains(constraint, true) ?: false) ||
            (manga.artist?.contains(constraint, true) ?: false) ||
            if (constraint.contains(",")) {
                val genres = manga.getGenres()
                constraint.split(",").all { containsGenre(it.trim(), genres) }
            } else {
                containsGenre(constraint, manga.getGenres())
            }
    }

    private fun containsGenre(tag: String, genres: List<String>?): Boolean {
        if (tag.trim().isEmpty()) return true
        return if (tag.startsWith("-")) {
            genres?.find { it.trim().equals(tag.substringAfter("-"), ignoreCase = true) } == null
        } else {
            genres?.find { it.trim().equals(tag, ignoreCase = true) } != null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is LibraryItem) {
            return manga.id == other.manga.id && manga.category == other.manga.category
        }
        return false
    }

    override fun hashCode(): Int {
        var result = manga.id!!.hashCode()
        result = 31 * result + (header?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val LAYOUT_LIST = 0
        const val LAYOUT_COMPACT_GRID = 1
        const val LAYOUT_COMFORTABLE_GRID = 2
        const val LAYOUT_COVER_ONLY_GRID = 3
    }
}
