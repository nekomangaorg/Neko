package eu.kanade.tachiyomi.ui.source

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [SourceController].
 */
class SourceAdapter(val controller: SourceController) :
        FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val browseClickListener: OnBrowseClickListener = controller

    /**
     * Listener for latest item clicks.
     */
    val latestClickListener: OnLatestClickListener = controller

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        controller.hideCatalogue(position)
    }

    /**
     * Listener which should be called when user clicks browse.
     * Note: Should only be handled by [SourceController]
     */
    interface OnBrowseClickListener {
        fun onBrowseClick(position: Int)
    }

    /**
     * Listener which should be called when user clicks latest.
     * Note: Should only be handled by [SourceController]
     */
    interface OnLatestClickListener {
        fun onLatestClick(position: Int)
    }
}
