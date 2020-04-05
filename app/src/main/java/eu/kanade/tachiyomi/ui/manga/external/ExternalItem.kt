package eu.kanade.tachiyomi.ui.manga.external

import android.view.View
import android.widget.ImageView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.external.ExternalLink

open class ExternalItem(val externalLink: ExternalLink) : AbstractItem<ExternalItem.ViewHolder>() {

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int
        get() = R.id.external_items_id

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int
        get() = R.layout.external_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ExternalItem>(view) {
        var logo = view.findViewById<ImageView>(R.id.external_logo)!!
        var container = view.findViewById<View>(R.id.external_logo_container)!!
        override fun bindView(item: ExternalItem, payloads: List<Any>) {
            container.setBackgroundColor(item.externalLink.getLogoColor())
            logo.setImageResource(item.externalLink.getLogo())
            logo.contentDescription = item.externalLink.name
        }

        override fun unbindView(item: ExternalItem) {
        }
    }
}
