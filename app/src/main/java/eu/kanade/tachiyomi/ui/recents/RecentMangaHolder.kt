package eu.kanade.tachiyomi.ui.recents

import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.visibleIf
import java.util.Date
import kotlinx.android.synthetic.main.download_button.*
import kotlinx.android.synthetic.main.recent_manga_item.*

class RecentMangaHolder(
    view: View,
    val adapter: RecentMangaAdapter
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        cover_thumbnail.setOnClickListener { adapter.delegate.onCoverClick(adapterPosition) }
        download_button.setOnClickListener { downloadOrRemoveMenu() }
    }

    private fun downloadOrRemoveMenu() {
        val chapter = adapter.getItem(adapterPosition) as? RecentMangaItem ?: return
        if (chapter.status == Download.NOT_DOWNLOADED || chapter.status == Download.ERROR) {
            adapter.delegate.downloadChapter(adapterPosition)
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
                        R.id.action_delete -> adapter.delegate.downloadChapter(adapterPosition)
                        R.id.action_start -> adapter.delegate.startDownloadNow(adapterPosition)
                    }
                    true
                }

                // Finally show the PopupMenu
                popup.show()
            }
        }
    }

    fun bind(item: RecentMangaItem) {
        download_button.visibleIf(item.mch.manga.source != LocalSource.ID)
        title.text = item.mch.manga.title
        val holder = (adapter.delegate as RecentsHolder)
        val isSearch =
            (holder.adapter.getItem(holder.adapterPosition) as RecentsItem).recentType == RecentsItem.SEARCH
        subtitle.text = item.chapter.name
        body.text = if (isSearch) when {
            item.chapter.id != item.mch.chapter.id -> body.context.getString(
                R.string.last_read_chapter_x, adapter.decimalFormat.format(
                    item.mch.chapter.chapter_number
                ) + " (${DateUtils.getRelativeTimeSpanString(
                    item.mch.history.last_read, Date().time, DateUtils.MINUTE_IN_MILLIS
                )})"
            )
            item.mch.history.id == null -> body.context.getString(
                R.string.uploaded_x, DateUtils.getRelativeTimeSpanString(
                    item.chapter.date_upload, Date().time, DateUtils.HOUR_IN_MILLIS
                ).toString()
            )
            else -> body.context.getString(
                R.string.last_read_x, DateUtils.getRelativeTimeSpanString(
                    item.mch.history.last_read, Date().time, DateUtils.MINUTE_IN_MILLIS
                ).toString()
            )
        } else when {
            item.chapter.id != item.mch.chapter.id -> body.context.getString(
                R.string.last_read_chapter_x, adapter.decimalFormat.format(
                    item.mch.chapter.chapter_number
                )
            )
            item.mch.history.id == null -> DateUtils.getRelativeTimeSpanString(
                item.chapter.date_upload, Date().time, DateUtils.HOUR_IN_MILLIS
            ).toString()
            item.chapter.pages_left > 0 -> itemView.resources.getQuantityString(
                R.plurals.pages_left, item.chapter.pages_left, item.chapter.pages_left
            )
            else -> ""
        }
        adapter.delegate.setCover(item.mch.manga, cover_thumbnail)
        notifyStatus(
            if (adapter.isSelected(adapterPosition)) Download.CHECKED else item.status,
            item.progress
        )
    }

    fun notifyStatus(status: Int, progress: Int) = with(download_button) {
        setDownloadStatus(status, progress)
    }

    override fun getFrontView(): View {
        return front_view
    }

    override fun getRearRightView(): View {
        return right_view
    }
}
