package eu.kanade.tachiyomi.ui.recents

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R

class RecentsItem(val recentType: Int, val mangaList: List<RecentMangaItem>) :
    AbstractFlexibleItem<RecentsHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.recents_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): RecentsHolder {
        return RecentsHolder(view, adapter as RecentsAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: RecentsHolder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {

        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is RecentsItem) {
            return recentType == other.recentType
        }
        return false
    }

    override fun hashCode(): Int {
        return recentType.hashCode()
    }

    companion object {
        const val CONTINUE_READING = 0
        const val NEW_CHAPTERS = 1
        const val NEWLY_ADDED = 2
        const val SEARCH = 3
    }
}
