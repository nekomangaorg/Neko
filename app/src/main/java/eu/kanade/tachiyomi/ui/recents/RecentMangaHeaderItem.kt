package eu.kanade.tachiyomi.ui.recents

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.library.LibraryHeaderItem
import kotlinx.android.synthetic.main.recents_header_item.*

class RecentMangaHeaderItem(val recentsType: Int) :
    AbstractHeaderItem<RecentMangaHeaderItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.recents_header_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): Holder {
        return Holder(view, adapter as RecentMangaAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(recentsType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LibraryHeaderItem) {
            return recentsType == recentsType
        }
        return false
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun isSwipeable(): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return recentsType.hashCode()
    }

    class Holder(val view: View, adapter: RecentMangaAdapter) : BaseFlexibleViewHolder(view, adapter,
        true) {

        fun bind(recentsType: Int) {
            title.setText(when (recentsType) {
                CONTINUE_READING -> R.string.continue_reading
                NEW_CHAPTERS -> R.string.new_chapters
                NEWLY_ADDED -> R.string.new_additions
                else -> R.string.continue_reading
            })
        }
    }

    companion object {
        const val CONTINUE_READING = 0
        const val NEW_CHAPTERS = 1
        const val NEWLY_ADDED = 2
    }
}
