package eu.kanade.tachiyomi.ui.catalogue.filter

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractExpandableHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.flexibleadapter.items.ISectionable
import eu.davidea.viewholders.ExpandableViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Filter

class GroupItem(val filter: Filter.Group<*>) : AbstractExpandableHeaderItem<GroupItem.Holder, ISectionable<*, *>>() {

    init {
        isExpanded = false
    }

    override fun getLayoutRes(): Int {
        return R.layout.navigation_view_group
    }

    override fun getItemViewType(): Int {
        return 101
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder? {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: MutableList<Any>) {
        holder.title.text = filter.name

        val icon = if (isExpanded)
            CommunityMaterial.Icon.cmd_chevron_down
        else
            CommunityMaterial.Icon.cmd_chevron_right

        holder.icon
                .setImageDrawable(IconicsDrawable(holder.contentView.context)
                        .icon(icon)
                        .colorInt(Color.WHITE)
                        .sizeDp(16))

        holder.itemView.setOnClickListener(holder)

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return filter == (other as GroupItem).filter
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }


    open class Holder(view: View, adapter: FlexibleAdapter<*>) : ExpandableViewHolder(view, adapter, true) {

        val title: TextView = itemView.findViewById(R.id.title)
        val icon: ImageView = itemView.findViewById(R.id.expand_icon)

        override fun shouldNotifyParentOnClick(): Boolean {
            return true
        }

    }
}