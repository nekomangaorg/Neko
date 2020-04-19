package eu.kanade.tachiyomi.ui.source.global_search

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource

/**
 * Item that contains search result information.
 *
 * @param source the source for the search results.
 * @param results the search results.
 * @param highlighted whether this search item should be highlighted/marked in the catalogue search view.
 */
class SourceSearchItem(val source: CatalogueSource, val results: List<SourceSearchCardItem>?, val highlighted: Boolean = false) :
    AbstractFlexibleItem<SourceSearchHolder>() {

    /**
     * Set view.
     *
     * @return id of view
     */
    override fun getLayoutRes(): Int {
        return R.layout.source_global_search_controller_card
    }

    /**
     * Create view holder (see [SourceSearchAdapter].
     *
     * @return holder of view.
     */
    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): SourceSearchHolder {
        return SourceSearchHolder(view, adapter as SourceSearchAdapter)
    }

    /**
     * Bind item to view.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: SourceSearchHolder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(this)
    }

    /**
     * Used to check if two items are equal.
     *
     * @return items are equal?
     */
    override fun equals(other: Any?): Boolean {
        if (other is SourceSearchItem) {
            return source.id == other.source.id
        }
        return false
    }

    /**
     * Return hash code of item.
     *
     * @return hashcode
     */
    override fun hashCode(): Int {
        return source.id.toInt()
    }
}
