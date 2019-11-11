package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import androidx.core.content.ContextCompat
import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.getResourceColor
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat

class ChaptersAdapter(
        controller: ChaptersController,
        context: Context
) : FlexibleAdapter<ChapterItem>(null, controller, true) {

    var items: List<ChapterItem> = emptyList()

    val menuItemListener: OnMenuItemClickListener = controller

    val readColor = context.getResourceColor(android.R.attr.textColorHint)

    val unreadColor = context.getResourceColor(android.R.attr.textColorPrimary)

    val bookmarkedColor = ContextCompat.getColor(context, R.color.mangadex_orange)

    val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols()
            .apply { decimalSeparator = '.' })

    val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun updateDataSet(items: List<ChapterItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: ChapterItem): Int {
        return items.indexOf(item)
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(position: Int, item: MenuItem)
    }

}
