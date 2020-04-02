package eu.kanade.tachiyomi.ui.recents

import android.text.format.DateUtils
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterHolder
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.download_button.*
import kotlinx.android.synthetic.main.recent_manga_item.*
import java.util.Date

class RecentMangaHolder(
    view: View,
    val adapter: RecentMangaAdapter
) : BaseChapterHolder(view, adapter) {

    init {
        cover_thumbnail.setOnClickListener { adapter.delegate.onCoverClick(adapterPosition) }
    }

    fun bind(item: RecentMangaItem) {
        download_button.visibility = View.VISIBLE
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

    fun notifyStatus(status: Int, progress: Int) =
        download_button.setDownloadStatus(status, progress)

    override fun getFrontView(): View {
        return front_view
    }

    override fun getRearRightView(): View {
        return right_view
    }
}
