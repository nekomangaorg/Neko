package eu.kanade.tachiyomi.ui.library

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.visible

class LibraryHeaderItem(val category: Category) : AbstractHeaderItem<LibraryHeaderItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.library_category_header_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): Holder {
        return Holder(view, adapter as LibraryCategoryAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LibraryHeaderItem) {
            return category.id == other.category.id
        }
        return false
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun isSelectable(): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return -(category.id!!)
    }

    class Holder(view: View, private val adapter: LibraryCategoryAdapter) :
        FlexibleViewHolder(view, adapter, true) {

        private val sectionText: TextView = view.findViewById(R.id.category_title)
        private val sortText: TextView = view.findViewById(R.id.category_sort)
        private val updateButton: MaterialButton = view.findViewById(R.id.update_button)
        private val catProgress: ProgressBar = view.findViewById(R.id.cat_progress)

        init {
            updateButton.setOnClickListener { addCategoryToUpdate() }
            sortText.setOnClickListener { showCatSortOptions() }
        }

        fun bind(item: LibraryHeaderItem) {
            sectionText.text = item.category.name
            sortText.text = itemView.context.getString(
                when (item.category.sortingMode()) {
                    LibrarySort.LAST_UPDATED -> R.string.action_sort_last_updated
                    LibrarySort.DRAG_AND_DROP -> R.string.action_sort_drag_and_drop
                    LibrarySort.TOTAL -> R.string.action_sort_total
                    LibrarySort.UNREAD -> R.string.action_filter_unread
                    LibrarySort.LAST_READ -> R.string.action_sort_last_read
                    LibrarySort.ALPHA -> R.string.title
                    else -> R.string.action_sort_drag_and_drop
                }
            )


            when {
                item.category.id == -1 -> {
                    updateButton.invisible()
                    catProgress.gone()
                }
                LibraryUpdateService.categoryInQueue(item.category.id) -> {
                    catProgress.visible()
                    updateButton.invisible()
                }
                else -> {
                    catProgress.gone()
                    updateButton.visible()
                }
            }
        }

        private fun addCategoryToUpdate() {
            if (adapter.libraryListener.updateCategory(adapterPosition)) {
                catProgress.visible()
                updateButton.invisible()
            }
        }

        private fun showCatSortOptions() {
            val category =
                (adapter.getItem(adapterPosition) as? LibraryHeaderItem)?.category ?: return
            // Create a PopupMenu, giving it the clicked view for an anchor
            val popup = PopupMenu(itemView.context, sortText)

            // Inflate our menu resource into the PopupMenu's Menu
            popup.menuInflater.inflate(
                if (category.id == -1) R.menu.main_sort
                else R.menu.cat_sort, popup.menu)

            // Set a listener so we are notified if a menu item is clicked
            popup.setOnMenuItemClickListener { menuItem ->
                onCatSortClicked(category, menuItem.itemId)
                true
            }

            val sortingMode = category.sortingMode()
            val currentItem = if (sortingMode == null) null
            else popup.menu.findItem(
                when (sortingMode) {
                    LibrarySort.DRAG_AND_DROP -> R.id.action_drag_and_drop
                    LibrarySort.TOTAL -> R.id.action_total_chaps
                    LibrarySort.LAST_READ -> R.id.action_last_read
                    LibrarySort.UNREAD -> R.id.action_unread
                    LibrarySort.LAST_UPDATED -> R.id.action_update
                    else -> R.id.action_alpha
                }
            )

            if (sortingMode != null && popup.menu is MenuBuilder) {
                val m = popup.menu as MenuBuilder
                m.setOptionalIconsVisible(true)
            }

            currentItem?.icon = tintVector(
                when {
                    sortingMode == LibrarySort.DRAG_AND_DROP -> R.drawable.ic_check_white_24dp
                    category.isAscending() -> R.drawable.ic_arrow_up_white_24dp
                    else -> R.drawable.ic_arrow_down_white_24dp
                }
            )

            // Finally show the PopupMenu
            popup.show()
        }

        private fun tintVector(resId: Int): Drawable? {
            return ContextCompat.getDrawable(itemView.context, resId)?.mutate()?.apply {
                setTint(itemView.context.getResourceColor(android.R.attr.colorAccent))
            }
        }

        private fun onCatSortClicked(category: Category, menuId: Int?) {
            val modType = if (menuId == null) {
                val t = (category.mangaSort?.minus('a') ?: 0) + 1
                if (t % 2 != 0) t + 1
                else t - 1
            }
            else {
                val order = when (menuId) {
                    R.id.action_total_chaps -> 4
                    R.id.action_last_read -> 3
                    R.id.action_unread -> 2
                    R.id.action_update -> 1
                    else -> 0
                }
                if (order == category.catSortingMode()) {
                    onCatSortClicked(category, null)
                    return
                }
                (2 * order + 1)
            }
            adapter.libraryListener.sortCategory(category.id!!, modType)
        }
    }
}