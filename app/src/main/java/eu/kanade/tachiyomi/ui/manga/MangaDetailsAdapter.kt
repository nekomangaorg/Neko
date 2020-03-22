package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.getResourceColor
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import uy.kohesive.injekt.injectLazy

class MangaDetailsAdapter(
    val controller: MangaDetailsController,
    context: Context
) : FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val preferences: PreferencesHelper by injectLazy()

    var items: List<ChapterItem> = emptyList()

    val coverListener: MangaHeaderInterface = controller

    val readColor = context.getResourceColor(android.R.attr.textColorHint)

    val unreadColor = context.getResourceColor(android.R.attr.textColorPrimary)

    val bookmarkedColor = context.getResourceColor(R.attr.colorAccent)

    val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols()
            .apply { decimalSeparator = '.' })

    fun setChapters(items: List<ChapterItem>?) {
        this.items = items ?: emptyList()
        performFilter()
    }

    fun indexOf(item: ChapterItem): Int {
        return items.indexOf(item)
    }

    fun unlock() {
        val activity = controller.activity as? FragmentActivity ?: return
        SecureActivityDelegate.promptLockIfNeeded(activity)
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(items)
        } else {
            updateDataSet(items.filter { it.name.contains(s, true) ||
                it.scanlator?.contains(s, true) == true })
        }
    }

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        when (direction) {
            ItemTouchHelper.RIGHT -> controller.bookmarkChapter(position)
            ItemTouchHelper.LEFT -> controller.toggleReadChapter(position)
        }
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
        fun showTrackingSheet()
        fun startDownloadRange(position: Int)
    }
}
