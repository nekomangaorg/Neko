package eu.kanade.tachiyomi.ui.recent_updates

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

class RecentChaptersAdapter(val controller: RecentChaptersController) :
        FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val coverClickListener: OnCoverClickListener = controller
    var recents = emptyList<RecentChapterItem>()

    init {
        setDisplayHeadersAtStartUp(true)
        //setStickyHeaders(true)
    }

    fun setItems(recents: List<RecentChapterItem>) {
        this.recents = recents
        performFilter()
    }

    fun performFilter() {
        val s = getFilter(String::class.java)
        if (s.isNullOrBlank()) {
            updateDataSet(recents)
        }
        else {
            updateDataSet(recents.filter { it.filter(s) })
        }
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}