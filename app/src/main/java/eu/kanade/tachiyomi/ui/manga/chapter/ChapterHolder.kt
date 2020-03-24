package eu.kanade.tachiyomi.ui.manga.chapter

import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.manga.MangaDetailsAdapter
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visibleIf
import java.util.Date
import kotlinx.android.synthetic.main.chapters_item.*
import kotlinx.android.synthetic.main.download_button.*

class ChapterHolder(
    private val view: View,
    private val adapter: MangaDetailsAdapter
) : BaseFlexibleViewHolder(view, adapter) {

    private var localSource = false
    init {
        download_button.setOnClickListener { downloadOrRemoveMenu() }
        download_button.setOnLongClickListener {
            adapter.coverListener.startDownloadRange(adapterPosition)
            true
        }
    }

    private fun downloadOrRemoveMenu() {
        val chapter = adapter.getItem(adapterPosition) as? ChapterItem ?: return
        if (chapter.status == Download.NOT_DOWNLOADED || chapter.status == Download.ERROR) {
            adapter.coverListener.downloadChapter(adapterPosition)
        } else {
            download_button.post {
                // Create a PopupMenu, giving it the clicked view for an anchor
                val popup = PopupMenu(download_button.context, download_button)

                // Inflate our menu resource into the PopupMenu's Menu
                popup.menuInflater.inflate(R.menu.chapter_download, popup.menu)

                // Hide download and show delete if the chapter is downloaded
                if (chapter.status != Download.DOWNLOADED) popup.menu.findItem(R.id.action_delete).title = download_button.context.getString(
                    R.string.action_cancel
                )

                // Set a listener so we are notified if a menu item is clicked
                popup.setOnMenuItemClickListener { _ ->
                    adapter.coverListener.downloadChapter(adapterPosition)
                    true
                }

                // Finally show the PopupMenu
                popup.show()
            }
        }
    }

    fun bind(item: ChapterItem, manga: Manga) {
        val chapter = item.chapter
        val isLocked = item.isLocked
        chapter_title.text = when (manga.displayMode) {
            Manga.DISPLAY_NUMBER -> {
                val number = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
                itemView.context.getString(R.string.display_mode_chapter, number)
            }
            else -> chapter.name
        }

        localSource = manga.source == LocalSource.ID
        download_button.visibleIf(!localSource)

        if (isLocked) download_button.gone()

        // Set correct text color
        chapter_title.setTextColor(
            if (chapter.read && !isLocked) adapter.readColor else adapter.unreadColor
        )
        if (chapter.bookmark && !isLocked) chapter_title.setTextColor(adapter.bookmarkedColor)

        val statuses = mutableListOf<String>()

        if (chapter.date_upload > 0) {
            statuses.add(
                DateUtils.getRelativeTimeSpanString(
                    chapter.date_upload, Date().time, DateUtils.HOUR_IN_MILLIS
                ).toString()
            )
        }

        if (!chapter.read && chapter.last_page_read > 0 && chapter.pages_left > 0 && !isLocked) {
            statuses.add(
                itemView.resources.getQuantityString(
                    R.plurals.pages_left, chapter.pages_left, chapter.pages_left
                )
            )
        } else if (!chapter.read && chapter.last_page_read > 0 && !isLocked) {
            statuses.add(
                itemView.context.getString(
                    R.string.chapter_progress, chapter.last_page_read + 1
                )
            )
        }

        if (!chapter.scanlator.isNullOrBlank()) {
            statuses.add(chapter.scanlator!!)
        }

        read.setImageDrawable(
            ContextCompat.getDrawable(
                read.context, if (item.read) R.drawable.eye_off
                else R.drawable.eye
            )
        )
        bookmark.setImageDrawable(
            ContextCompat.getDrawable(
                read.context, if (item.bookmark) R.drawable.star_off
                else R.drawable.star
            )
        )

        chapter_scanlator.setTextColor(if (chapter.read) adapter.readColor else adapter.unreadColor)
        chapter_scanlator.text = statuses.joinToString(" • ")
        notifyStatus(
            if (adapter.isSelected(adapterPosition)) Download.CHECKED else item.status,
            item.isLocked,
            item.progress
        )
        resetFrontView()
    }

    override fun getFrontView(): View {
        return front_view
    }

    override fun getRearRightView(): View {
        return right_view
    }

    override fun getRearLeftView(): View {
        return left_view
    }

    private fun resetFrontView() {
        if (front_view.translationX == 0f) return
        itemView.post { adapter.notifyItemChanged(adapterPosition) }
    }

    fun notifyStatus(status: Int, locked: Boolean, progress: Int) = with(download_button) {
        if (locked) {
            gone()
            return
        }
        download_button.visibleIf(!localSource)
        setDownloadStatus(status, progress)
    }
}
