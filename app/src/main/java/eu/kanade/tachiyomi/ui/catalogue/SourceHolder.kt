package eu.kanade.tachiyomi.ui.catalogue

import android.view.View
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.roundTextIcon
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.catalogue_main_controller_card_item.*

class SourceHolder(view: View, val adapter: CatalogueAdapter) :
        BaseFlexibleViewHolder(view, adapter) {

    /*override val slice = Slice(card).apply {
        setColor(adapter.cardBackground)
    }

    override val viewToSlice: View
        get() = card*/

    init {
        source_latest.setOnClickListener {
            adapter.latestClickListener.onLatestClick(adapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source
        // setCardEdges(item)

        // Set source name
        title.text = source.name

        // Set circle letter image.
        itemView.post {
            val icon = source.icon()
            if (icon != null) edit_button.setImageDrawable(source.icon())
            else edit_button.roundTextIcon(source.name)
        }

        if (source.supportsLatest) {
            source_latest.visible()
        } else {
            source_latest.gone()
        }
    }
}
