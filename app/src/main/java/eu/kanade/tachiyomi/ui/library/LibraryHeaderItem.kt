package eu.kanade.tachiyomi.ui.library

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.f2prateek.rx.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.library_category_header_item.view.*

class LibraryHeaderItem(
    private val categoryF: (Int) -> Category,
    private val catId: Int,
    private val showFastScroll: Preference<Boolean>
) :
    AbstractHeaderItem<LibraryHeaderItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.library_category_header_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): Holder {
        return Holder(view, adapter as LibraryCategoryAdapter, showFastScroll.getOrDefault())
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(categoryF(catId))
    }

    val category: Category
        get() = categoryF(catId)

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

    class Holder(val view: View, private val adapter: LibraryCategoryAdapter, padEnd: Boolean) :
        FlexibleViewHolder(view, adapter, true) {

        private val sectionText: TextView = view.findViewById(R.id.category_title)
        private val sortText: TextView = view.findViewById(R.id.category_sort)
        private val updateButton: ImageView = view.findViewById(R.id.update_button)
        private val checkboxImage: ImageView = view.findViewById(R.id.checkbox)

        init {
            sortText.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = (if (padEnd && adapter.recyclerView.paddingEnd == 0) 12 else 2).dpToPx
            }
            updateButton.setOnClickListener { addCategoryToUpdate() }
            sortText.setOnClickListener { it.post { showCatSortOptions() } }
            checkboxImage.setOnClickListener { selectAll() }
            updateButton.drawable.mutate()
        }

        fun bind(category: Category) {
            sectionText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = (if (category.isFirst == true) 2 else 44).dpToPx
            }

            if (category.isFirst == true && category.isLast == true) sectionText.text = ""
            else sectionText.text = category.name
            sortText.text = itemView.context.getString(R.string.sort_by_,
                itemView.context.getString(
                when (category.sortingMode()) {
                    LibrarySort.LATEST_CHAPTER -> R.string.latest_chapter
                    LibrarySort.DRAG_AND_DROP ->
                        if (category.id == -1) R.string.category
                        else R.string.drag_and_drop
                    LibrarySort.TOTAL -> R.string.total_chapters
                    LibrarySort.UNREAD -> R.string.unread
                    LibrarySort.LAST_READ -> R.string.last_read
                    LibrarySort.ALPHA -> R.string.title
                    LibrarySort.DATE_ADDED -> R.string.date_added
                    else -> R.string.drag_and_drop
                }
            ))

            when {
                adapter.mode == SelectableAdapter.Mode.MULTI -> {
                    checkboxImage.visible()
                    updateButton.gone()
                    setSelection()
                }
                category.id == -1 -> {
                    checkboxImage.gone()
                    updateButton.gone()
                }
                LibraryUpdateService.categoryInQueue(category.id) -> {
                    checkboxImage.gone()
                    updateButton.drawable.setTint(ContextCompat.getColor(itemView.context,
                        R.color.material_on_surface_disabled))
                    updateButton.visible()
                }
                else -> {
                    checkboxImage.gone()
                    updateButton.drawable.setTint(itemView.context.getResourceColor(
                        R.attr.colorAccent))
                    updateButton.visible()
                }
            }
        }

        private fun addCategoryToUpdate() {
            if (adapter.libraryListener.updateCategory(adapterPosition)) {
                updateButton.drawable.setTint(ContextCompat.getColor(itemView.context,
                    R.color.material_on_surface_disabled))
            }
        }
        private fun showCatSortOptions() {
            val category =
                (adapter.getItem(adapterPosition) as? LibraryHeaderItem)?.category ?: return
            // Create a PopupMenu, giving it the clicked view for an anchor
            val popup = PopupMenu(itemView.context, view.category_sort)

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
                    LibrarySort.LATEST_CHAPTER -> R.id.action_update
                    LibrarySort.DATE_ADDED -> R.id.action_date_added
                    else -> R.id.action_alpha
                }
            )

            if (category.id == -1)
            popup.menu.findItem(R.id.action_drag_and_drop).title = contentView.context.getString(
                R.string.category
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
            } else {
                val order = when (menuId) {
                    R.id.action_drag_and_drop -> {
                        adapter.libraryListener.sortCategory(category.id!!, 'D' - 'a' + 1)
                        return
                    }
                    R.id.action_date_added -> 5
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

        private fun selectAll() {
            adapter.libraryListener.selectAll(adapterPosition)
        }

        fun setSelection() {
            val allSelected = adapter.libraryListener.allSelected(adapterPosition)
            val drawable =
                ContextCompat.getDrawable(contentView.context,
                if (allSelected) R.drawable.ic_check_circle_white_24dp else
                    R.drawable.ic_radio_button_unchecked_white_24dp)
            val tintedDrawable = drawable?.mutate()
            tintedDrawable?.setTint(ContextCompat.getColor(contentView.context,
                if (allSelected) R.color.colorAccent
                else R.color.gray_button))
            checkboxImage.setImageDrawable(tintedDrawable)
        }

        override fun onLongClick(view: View?): Boolean {
            super.onLongClick(view)
            return false
        }
    }
}
