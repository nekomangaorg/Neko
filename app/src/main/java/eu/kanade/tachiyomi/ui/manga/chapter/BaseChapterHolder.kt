package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import kotlinx.android.synthetic.main.download_button.*

open class BaseChapterHolder(
    view: View,
    private val adapter: BaseChapterAdapter<*>
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        download_button?.setOnClickListener { downloadOrRemoveMenu() }
    }

    private fun downloadOrRemoveMenu() {
        val chapter = adapter.getItem(adapterPosition) as? BaseChapterItem<*, *> ?: return
        if (chapter.status == Download.NOT_DOWNLOADED || chapter.status == Download.ERROR) {
            adapter.baseDelegate.downloadChapter(adapterPosition)
        } else {
            download_button.post {
                // Create a PopupMenu, giving it the clicked view for an anchor
                val popup = PopupMenu(download_button.context, download_button)

                // Inflate our menu resource into the PopupMenu's Menu
                popup.menuInflater.inflate(R.menu.chapter_download, popup.menu)

                popup.menu.findItem(R.id.action_start).isVisible = chapter.status == Download.QUEUE

                // Hide download and show delete if the chapter is downloaded
                if (chapter.status != Download.DOWNLOADED) popup.menu.findItem(R.id.action_delete).title = download_button.context.getString(
                    R.string.action_cancel
                )

                // Set a listener so we are notified if a menu item is clicked
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_delete -> adapter.baseDelegate.downloadChapter(adapterPosition)
                        R.id.action_start -> adapter.baseDelegate.startDownloadNow(adapterPosition)
                    }
                    true
                }

                // Finally show the PopupMenu
                popup.show()
            }
        }
    }
}
