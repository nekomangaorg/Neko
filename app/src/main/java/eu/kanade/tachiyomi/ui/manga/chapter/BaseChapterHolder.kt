package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.widget.cascadeMenuStyler
import me.saket.cascade.CascadePopupMenu

open class BaseChapterHolder(
    view: View,
    private val adapter: BaseChapterAdapter<*>,
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        view.findViewById<View>(R.id.download_button)?.setOnClickListener { downloadOrRemoveMenu() }
    }

    private fun downloadOrRemoveMenu() {
        val chapter = adapter.getItem(flexibleAdapterPosition) as? BaseChapterItem<*, *> ?: return
        val downloadButton = itemView.findViewById<View>(R.id.download_button) ?: return

        if (chapter.status == Download.State.NOT_DOWNLOADED || chapter.status == Download.State.ERROR) {
            adapter.baseDelegate.downloadChapter(flexibleAdapterPosition)
        } else {
            downloadButton.post {
                // Create a PopupMenu, giving it the clicked view for an anchor
                val popup = CascadePopupMenu(
                    downloadButton.context,
                    downloadButton,
                    styler = cascadeMenuStyler(downloadButton.context)
                )

                // Inflate our menu resource into the PopupMenu's Menu
                popup.inflate(R.menu.chapter_download)

                popup.menu.findItem(R.id.action_start).isVisible =
                    chapter.status == Download.State.QUEUE

                // Hide download and show delete if the chapter is downloaded
                if (chapter.status != Download.State.DOWNLOADED) popup.menu.findItem(R.id.action_delete).title =
                    downloadButton.context.getString(
                        R.string.cancel
                    )

                // Set a listener so we are notified if a menu item is clicked
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_delete -> adapter.baseDelegate.downloadChapter(
                            flexibleAdapterPosition
                        )
                        R.id.action_start -> adapter.baseDelegate.startDownloadNow(
                            flexibleAdapterPosition
                        )
                    }
                    true
                }

                // Finally show the PopupMenu
                popup.show()
            }
        }
    }
}
