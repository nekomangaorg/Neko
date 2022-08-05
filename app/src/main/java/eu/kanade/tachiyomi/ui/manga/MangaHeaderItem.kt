package eu.kanade.tachiyomi.ui.manga

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga

class MangaHeaderItem(val manga: Manga, var startExpanded: Boolean) :
    AbstractFlexibleItem<MangaHeaderHolder>() {

    var isChapterHeader = false
    var isLocked = false
    var isTablet = false

    override fun getLayoutRes(): Int {
        return R.layout.manga_header_item
    }

    override fun isSelectable(): Boolean {
        return false
    }

    override fun isSwipeable(): Boolean {
        return false
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): MangaHeaderHolder {
        return MangaHeaderHolder(view, adapter as MangaDetailsAdapter, startExpanded, isTablet)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: MangaHeaderHolder,
        position: Int,
        payloads: MutableList<Any?>?,
    ) {
        holder.bind(this, manga)
    }

    override fun equals(other: Any?): Boolean {
        return (this === other)
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }
}
