package eu.kanade.tachiyomi.ui.download

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter storing a list of downloads.
 *
 * @param context the context of the fragment containing this adapter.
 */
class DownloadAdapter(controller: DownloadController) : FlexibleAdapter<DownloadItem>(null, controller,
    true) {

    /**
     * Listener called when an item of the list is released.
     */
    val onItemReleaseListener: OnItemReleaseListener = controller

    /**
     * Listener called when an item of the list is deleted.
     */
    val onItemDeleteListener: OnItemDeleteListener = controller

    interface OnItemReleaseListener {
        /**
         * Called when an item of the list is released.
         */
        fun onItemReleased(position: Int)
    }

    interface OnItemDeleteListener {
        /**
         * Called when an item of the list is has delete clicked
         */
        fun onItemDeleted(position: Int)
    }
}
