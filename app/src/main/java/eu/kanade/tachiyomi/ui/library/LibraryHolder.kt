package eu.kanade.tachiyomi.ui.library

import android.view.View
import androidx.core.content.ContextCompat
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Generic class used to hold the displayed data of a manga in the library.
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to the single tap and long tap events.
 */

abstract class LibraryHolder(
        view: View,
    val adapter: LibraryCategoryAdapter
) : BaseFlexibleViewHolder(view, adapter) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    abstract fun onSetValues(item: LibraryItem)


    /**
     * Called when an item is released.
     *
     * @param position The position of the released item.
     */
    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        (adapter as? LibraryCategoryAdapter)?.onItemReleaseListener?.onItemReleased(position)
    }

    protected fun convertColor(color: Int):String {
        return Integer.toHexString(color and 0x00ffffff)
    }
}
