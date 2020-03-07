package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsPresenter
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.getResourceColor
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class ChaptersAdapter(
        val controller: BaseController,
        context: Context
) : FlexibleAdapter<ChapterItem>(null, controller, true) {

    val preferences: PreferencesHelper by injectLazy()

    var items: List<ChapterItem> = emptyList()

    val menuItemListener: OnMenuItemClickListener? = controller as? OnMenuItemClickListener
    val coverListener: MangaHeaderInterface? = controller as? MangaHeaderInterface

    val readColor = context.getResourceColor(android.R.attr.textColorHint)

    val unreadColor = context.getResourceColor(android.R.attr.textColorPrimary)

    val bookmarkedColor = context.getResourceColor(R.attr.colorAccent)

    val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols()
            .apply { decimalSeparator = '.' })

    val dateFormat: DateFormat = preferences.dateFormat().getOrDefault()

    override fun updateDataSet(items: List<ChapterItem>?) {
        this.items = items ?: emptyList()
        super.updateDataSet(items)
    }

    fun indexOf(item: ChapterItem): Int {
        return items.indexOf(item)
    }

    fun unlock() {
        val activity = controller.activity as? FragmentActivity ?: return
        SecureActivityDelegate.promptLockIfNeeded(activity)
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(position: Int, item: MenuItem)
    }

    interface MangaHeaderInterface {
        fun coverColor(): Int?
        fun mangaPresenter(): MangaDetailsPresenter
        fun prepareToShareManga()
        fun openInWebView()
        fun readNextChapter()
        fun downloadChapter(position: Int)
        fun topCoverHeight(): Int
        fun tagClicked(text: String)
        fun showChapterFilter()
        fun favoriteManga(longPress: Boolean)
        fun copyToClipboard(content: String, label: Int)
        fun zoomImageFromThumb(thumbView: View)
    }
}
