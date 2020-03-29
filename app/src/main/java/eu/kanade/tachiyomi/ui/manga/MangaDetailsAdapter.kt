package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterAdapter
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.util.system.getResourceColor
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class MangaDetailsAdapter(
    val controller: MangaDetailsController,
    context: Context
) : BaseChapterAdapter<IFlexible<*>>(controller) {

    val preferences: PreferencesHelper by injectLazy()

    var items: List<ChapterItem> = emptyList()

    private var isAnimating = false
    val delegate: MangaDetailsInterface = controller

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

    fun indexOf(chapterId: Long): Int {
        return currentItems.indexOfFirst { it is ChapterItem && it.id == chapterId }
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(items, isAnimating)
        } else {
            updateDataSet(items.filter { it.name.contains(s, true) ||
                it.scanlator?.contains(s, true) == true }, isAnimating)
        }
        isAnimating = false
    }

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        isAnimating = true
        when (direction) {
            ItemTouchHelper.RIGHT -> controller.bookmarkChapter(position)
            ItemTouchHelper.LEFT -> controller.toggleReadChapter(position)
        }
    }

    interface MangaDetailsInterface : MangaHeaderInterface, DownloadInterface

    interface MangaHeaderInterface {
        fun coverColor(): Int?
        fun mangaPresenter(): MangaDetailsPresenter
        fun prepareToShareManga()
        fun openInWebView()
        fun startDownloadRange(position: Int)
        fun readNextChapter()
        fun topCoverHeight(): Int
        fun tagClicked(text: String)
        fun showChapterFilter()
        fun favoriteManga(longPress: Boolean)
        fun copyToClipboard(content: String, label: Int)
        fun zoomImageFromThumb(thumbView: View)
        fun showTrackingSheet()
    }
}
