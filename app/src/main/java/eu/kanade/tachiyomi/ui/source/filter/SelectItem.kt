package eu.kanade.tachiyomi.ui.source.filter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.widget.MaterialSpinnerView

open class SelectItem(val filter: Filter.Select<*>) : AbstractFlexibleItem<SelectItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.navigation_view_spinner
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: Holder, position: Int, payloads: MutableList<Any?>?) {
        holder.spinnerView.title = filter.name + ": "

        holder.spinnerView.setEntries(filter.values.map { it.toString() })
        holder.spinnerView.setSelection(filter.state)
        holder.spinnerView.onItemSelectedListener = { pos ->
            filter.state = pos
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return filter == (other as SelectItem).filter
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>) : FlexibleViewHolder(view, adapter) {
        val spinnerView: MaterialSpinnerView = itemView.findViewById(R.id.nav_view_item)
    }
}
