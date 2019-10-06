package eu.kanade.tachiyomi.ui.catalogue.filter

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.CheckedTextView
import com.google.android.material.R
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.util.dpToPx
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.R as TR

open class TriStateItem(val filter: Filter.TriState) : AbstractFlexibleItem<TriStateItem.Holder>() {

    override fun getLayoutRes(): Int {
        return TR.layout.navigation_view_checkedtext
    }

    override fun getItemViewType(): Int {
        return 103
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<*>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<*>, holder: Holder, position: Int, payloads: List<Any?>?) {
        val view = holder.text
        view.text = filter.name

        fun getIcon(): Drawable {
            val icon = when (filter.state) {
                Filter.TriState.STATE_INCLUDE -> CommunityMaterial.Icon.cmd_check_box_outline
                Filter.TriState.STATE_EXCLUDE -> CommunityMaterial.Icon.cmd_close_box_outline
                else -> CommunityMaterial.Icon.cmd_checkbox_blank_outline
            }
            val color = if (filter.state == Filter.TriState.STATE_INCLUDE)
                view.context.getResourceColor(R.attr.colorAccent)
            else
                view.context.getResourceColor(android.R.attr.textColorSecondary)

            return IconicsDrawable(view.context).icon(icon).sizeDp(18).color(color)
        }




        view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
        holder.itemView.setOnClickListener {
            filter.state = (filter.state + 1) % 3
            view.setCompoundDrawablesWithIntrinsicBounds(getIcon(), null, null, null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return filter == (other as TriStateItem).filter
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {

        val text: CheckedTextView = itemView.findViewById(TR.id.nav_view_item)

        init {
            // Align with native checkbox
            text.setPadding(4.dpToPx, 0, 0, 0)
            text.compoundDrawablePadding = 20.dpToPx
        }
    }

}