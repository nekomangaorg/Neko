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
    val presenter = controller.presenter

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

    fun getSectionText(position: Int): String? {
        val chapter = getItem(position) as? ChapterItem ?: return null
        if (position == itemCount - 1) return "-"
        return when (presenter.scrollType) {
            MangaDetailsPresenter.MULTIPLE_VOLUMES, MangaDetailsPresenter.MULTIPLE_SEASONS ->
                presenter.getGroupNumber(chapter)?.toString() ?: "*"
            MangaDetailsPresenter.HUNDREDS_OF_CHAPTERS ->
                if (chapter.chapter_number < 0) "*"
            else (chapter.chapter_number / 100).toInt().toString()
            MangaDetailsPresenter.TENS_OF_CHAPTERS ->
                if (chapter.chapter_number < 0) "*"
                else (chapter.chapter_number / 10).toInt().toString()
            else -> null
        }
    }

    fun getFullText(position: Int): String {
        val chapter =
            getItem(position) as? ChapterItem ?: return recyclerView.context.getString(R.string.top)
        if (position == itemCount - 1) return recyclerView.context.getString(R.string.bottom)
        return when (val scrollType = presenter.scrollType) {
            MangaDetailsPresenter.MULTIPLE_VOLUMES, MangaDetailsPresenter.MULTIPLE_SEASONS -> {
                val volume = presenter.getGroupNumber(chapter)
                if (volume != null) recyclerView.context.getString(
                    if (scrollType == MangaDetailsPresenter.MULTIPLE_SEASONS) R.string.season_
                    else R.string.volume_, volume)
                else recyclerView.context.getString(R.string.unknown)
            }
            MangaDetailsPresenter.HUNDREDS_OF_CHAPTERS -> recyclerView.context.getString(
                R.string.chapters_, get100sRange(
                    chapter.chapter_number
                )
            )
            MangaDetailsPresenter.TENS_OF_CHAPTERS -> recyclerView.context.getString(
                R.string.chapters_, get10sRange(
                    chapter.chapter_number
                )
            )
            else -> recyclerView.context.getString(R.string.unknown)
        }
    }

    private fun get100sRange(value: Float): String {
        return when (value.toInt()) {
            in 0..99 -> "0-99"
            in 100..199 -> "100-199"
            in 200..299 -> "200-299"
            in 300..399 -> "300-399"
            in 400..499 -> "400-499"
            in 500..599 -> "500-599"
            in 600..699 -> "600-699"
            in 700..799 -> "700-799"
            in 800..899 -> "800-899"
            in 900..Int.MAX_VALUE -> "900+"
            else -> "None"
        }
    }

    private fun get10sRange(value: Float): String {
        return when (value.toInt()) {
            in 0..9 -> "0-9"
            in 10..19 -> "10-19"
            in 20..29 -> "20-29"
            in 30..39 -> "30-39"
            in 40..49 -> "40-49"
            in 50..59 -> "50-59"
            in 60..69 -> "60-69"
            in 70..79 -> "70-79"
            in 80..89 -> "80-89"
            in 80..89 -> "80-89"
            in 90..99 -> "90-99"
            in 100..109 -> "100-109"
            in 110..119 -> "110-119"
            in 120..129 -> "120-129"
            in 130..139 -> "130-139"
            in 140..149 -> "140-149"
            in 150..159 -> "150-159"
            in 160..169 -> "160-169"
            in 170..179 -> "170-179"
            in 180..189 -> "180-189"
            in 190..199 -> "190-199"
            in 190..199 -> "190-199"
            in 200..209 -> "200-209"
            in 210..219 -> "210-219"
            in 220..229 -> "220-229"
            in 230..239 -> "230-239"
            in 240..249 -> "240-249"
            in 250..259 -> "250-259"
            in 260..269 -> "260-269"
            in 270..279 -> "270-279"
            in 280..289 -> "280-289"
            in 290..299 -> "290-299"
            in 290..299 -> "290-299"
            in 300..Int.MAX_VALUE -> "300+"
            else -> recyclerView.context.getString(R.string.unknown)
        }
    }

    interface MangaDetailsInterface : MangaHeaderInterface, DownloadInterface

    interface MangaHeaderInterface {
        fun coverColor(): Int?
        fun mangaPresenter(): MangaDetailsPresenter
        fun prepareToShareManga()
        fun openSimilar()
        fun startDownloadRange(position: Int)
        fun readNextChapter()
        fun topCoverHeight(): Int
        fun tagClicked(text: String)
        fun showChapterFilter()
        fun favoriteManga(longPress: Boolean)
        fun copyToClipboard(content: String, label: Int)
        fun zoomImageFromThumb(thumbView: View)
        fun showTrackingSheet()
        fun showExternalSheet()
    }
}
