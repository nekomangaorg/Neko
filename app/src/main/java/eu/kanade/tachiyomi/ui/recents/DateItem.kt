package eu.kanade.tachiyomi.ui.recents

import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.util.Date
import org.nekomanga.R

class DateItem(val date: Date, val addedString: Boolean = false) :
    AbstractHeaderItem<DateItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.recent_chapters_section_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): Holder {
        return Holder(view, adapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(this)
    }

    override fun isSwipeable(): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DateItem) {
            return date == other.date
        }
        return false
    }

    override fun hashCode(): Int {
        return date.hashCode()
    }

    class Holder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>) :
        FlexibleViewHolder(view, adapter, true) {

        private val now = Date().time

        private val sectionText: TextView = view.findViewById(R.id.section_text)

        fun bind(item: DateItem) {
            val dateString =
                DateUtils.getRelativeTimeSpanString(item.date.time, now, DateUtils.DAY_IN_MILLIS)
            sectionText.text =
                if (item.addedString) itemView.context.getString(R.string.fetched_, dateString)
                else dateString
        }

        override fun onLongClick(view: View?): Boolean {
            super.onLongClick(view)
            return false
        }
    }
}
