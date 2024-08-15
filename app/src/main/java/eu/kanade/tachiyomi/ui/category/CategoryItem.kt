package eu.kanade.tachiyomi.ui.category

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.category.CategoryPresenter.Companion.CREATE_CATEGORY_ORDER
import org.nekomanga.R

/** Category item for a recycler view. */
class CategoryItem(val category: Category) : AbstractFlexibleItem<CategoryHolder>() {

    /** Whether this item is currently selected. */
    var isEditing = false

    /** Returns the layout resource for this item. */
    override fun getLayoutRes(): Int {
        return R.layout.categories_item
    }

    /**
     * Returns a new view holder for this item.
     *
     * @param view The view of this item.
     * @param adapter The adapter of this item.
     */
    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): CategoryHolder {
        return CategoryHolder(view, adapter as CategoryAdapter)
    }

    /**
     * Binds the given view holder with this item.
     *
     * @param adapter The adapter of this item.
     * @param holder The holder to bind.
     * @param position The position of this item in the adapter.
     * @param payloads List of partial changes.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: CategoryHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(category)
        holder.isEditing(isEditing)
    }

    /** Returns true if this item is draggable. */
    override fun isDraggable(): Boolean {
        return category.order != CREATE_CATEGORY_ORDER && !isEditing
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is CategoryItem) {
            return category.id == other.category.id
        }
        return false
    }

    override fun hashCode(): Int {
        return category.id!!
    }
}
