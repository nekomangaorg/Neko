package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractExpandableHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import org.nekomanga.R

data class DownloadHeaderItem(
    val id: Long,
    val name: String,
    val size: Int,
) : AbstractExpandableHeaderItem<DownloadHeaderHolder, DownloadItem>() {

    override fun getLayoutRes(): Int {
        return R.layout.download_header
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): DownloadHeaderHolder {
        return DownloadHeaderHolder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: DownloadHeaderHolder,
        position: Int,
        payloads: List<Any?>?,
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DownloadHeaderItem) {
            return id == other.id && name == other.name
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    init {
        isHidden = false
        isExpanded = true
        isSelectable = false
        isSwipeable = false
    }
}
