package eu.kanade.tachiyomi.ui.recent_updates

import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterAdapter

class RecentChaptersAdapter(val controller: RecentChaptersController) :
    BaseChapterAdapter<IFlexible<*>>(controller) {

    val coverClickListener: OnCoverClickListener = controller
    var recents = emptyList<RecentChapterItem>()
    private var isAnimating = false

    init {
        setDisplayHeadersAtStartUp(true)
        // setStickyHeaders(true)
    }

    fun setItems(recents: List<RecentChapterItem>) {
        this.recents = recents
        performFilter()
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(recents, isAnimating)
        } else {
            updateDataSet(recents.filter { it.filter(s) }, isAnimating)
        }
        isAnimating = false
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        isAnimating = true
        when (direction) {
            ItemTouchHelper.LEFT -> controller.toggleMarkAsRead(position)
        }
    }
}
