package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem

/**
 * Adapter storing a list of downloads.
 *
 * @param context the context of the fragment containing this adapter.
 */
class DownloadAdapter(controller: DownloadItemListener) : FlexibleAdapter<AbstractFlexibleItem<*>>(
    null,
    controller,
    true,
) {

    /**
     * Listener called when an item of the list is released.
     */
    val downloadItemListener: DownloadItemListener = controller

    interface DownloadItemListener {
        /**
         * Called when an item of the list is released.
         */
        fun onItemReleased(position: Int)
        fun onItemRemoved(position: Int)
        fun onMenuItemClick(position: Int, menuItem: MenuItem)
    }

    override fun shouldMove(fromPosition: Int, toPosition: Int): Boolean {
        // Don't let sub-items changing group
        return getHeaderOf(getItem(fromPosition)) == getHeaderOf(getItem(toPosition)) &&
            getItem(toPosition) !is DownloadSwipeTutorialItem
    }

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        downloadItemListener.onItemRemoved(position)
    }

    override fun onCreateBubbleText(position: Int): String {
        return when (val item = getItem(position)) {
            is DownloadHeaderItem -> item.name
            is DownloadItem -> item.download.manga.title
            else -> ""
        }
    }
}
