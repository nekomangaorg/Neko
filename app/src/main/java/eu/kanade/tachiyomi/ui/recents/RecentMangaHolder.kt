package eu.kanade.tachiyomi.ui.recents

import android.text.format.DateUtils
import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterHolder
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.download_button.*
import kotlinx.android.synthetic.main.recent_manga_item.*
import java.util.Date

class RecentMangaHolder(
    view: View,
    val adapter: RecentMangaAdapter
) : BaseChapterHolder(view, adapter) {

    init {
        cover_thumbnail?.setOnClickListener { adapter.delegate.onCoverClick(adapterPosition) }
    }

    fun bind(recentsType: Int) {
        when (recentsType) {
            RecentMangaHeaderItem.CONTINUE_READING -> {
                title.setText(R.string.view_history)
            }
            RecentMangaHeaderItem.NEW_CHAPTERS -> {
                title.setText(R.string.view_all_updates)
            }
        }
    }

    fun bind(item: RecentMangaItem) {
        download_button.visibleIf(item.mch.manga.source != LocalSource.ID)
        title.apply {
            text = item.chapter.name
            setTextColor(when {
                item.chapter.bookmark -> context.getResourceColor(R.attr.colorAccent)
                item.chapter.read -> context.getResourceColor(android.R.attr.textColorHint)
                else -> context.getResourceColor(android.R.attr.textColorPrimary)
            })
        }
        subtitle.apply {
            text = item.mch.manga.title
            setTextColor(when {
                item.chapter.read -> context.getResourceColor(android.R.attr.textColorHint)
                else -> context.getResourceColor(android.R.attr.textColorPrimary)
            })
        }
        val notValidNum = item.mch.chapter.chapter_number <= 0
        body.text = when {
            item.mch.chapter.id == null -> body.context.getString(
                R.string.added_, DateUtils.getRelativeTimeSpanString(
                    item.mch.manga.date_added, Date().time, DateUtils.MINUTE_IN_MILLIS
                ).toString()
            )
            item.mch.history.id == null -> body.context.getString(
                R.string.updated_, DateUtils.getRelativeTimeSpanString(
                    item.chapter.date_upload, Date().time, DateUtils.HOUR_IN_MILLIS
                ).toString()
            )
            item.chapter.id != item.mch.chapter.id ->
                body.context.getString(
                    R.string.read_, DateUtils.getRelativeTimeSpanString(
                        item.mch.history.last_read, Date().time, DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                ) + "\n" + body.context.getString(
                if (notValidNum) R.string.last_read_ else R.string.last_read_chapter_,
                if (notValidNum) item.mch.chapter.name else adapter.decimalFormat.format(item.mch.chapter.chapter_number)
            )
            item.chapter.pages_left > 0 && !item.chapter.read -> body.context.getString(
                R.string.read_, DateUtils.getRelativeTimeSpanString(
                    item.mch.history.last_read, Date().time, DateUtils.MINUTE_IN_MILLIS
                ).toString()
            ) + "\n" + itemView.resources.getQuantityString(
                R.plurals.pages_left, item.chapter.pages_left, item.chapter.pages_left
            )
            else -> body.context.getString(
                R.string.read_, DateUtils.getRelativeTimeSpanString(
                    item.mch.history.last_read, Date().time, DateUtils.MINUTE_IN_MILLIS
                ).toString()
            )
        }
        GlideApp.with(itemView.context).load(item.mch.manga).diskCacheStrategy(DiskCacheStrategy
            .AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(item.mch.manga.id!!).toString())).into(cover_thumbnail)
        notifyStatus(
            if (adapter.isSelected(adapterPosition)) Download.CHECKED else item.status,
            item.progress
        )
    }

    override fun onLongClick(view: View?): Boolean {
        super.onLongClick(view)
        val item = adapter.getItem(adapterPosition) as? RecentMangaItem ?: return false
        return item.mch.history.id != null
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
