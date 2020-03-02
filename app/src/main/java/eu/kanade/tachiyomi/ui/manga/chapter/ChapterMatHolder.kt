package eu.kanade.tachiyomi.ui.manga.chapter

import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaChapterHolder
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.chapters_mat_item.*
import kotlinx.android.synthetic.main.download_button.*
import java.util.Date

class ChapterMatHolder(
    private val view: View,
    private val adapter: ChaptersAdapter
) : MangaChapterHolder(view, adapter) {

    init {
        // We need to post a Runnable to show the popup to make sure that the PopupMenu is
        // correctly positioned. The reason being that the view may change position before the
        // PopupMenu is shown.
        //chapter_menu.setOnClickListener { it.post { showPopupMenu(it) } }
        download_button.setOnClickListener { downloadOrRemoveMenu() }
    }

    private fun downloadOrRemoveMenu() {
        val chapter = adapter.getItem(adapterPosition) ?: return
        if (chapter.status != Download.NOT_DOWNLOADED) {
            download_button.post {
                // Create a PopupMenu, giving it the clicked view for an anchor
                val popup = PopupMenu(download_button.context, download_button)

                // Inflate our menu resource into the PopupMenu's Menu
                popup.menuInflater.inflate(R.menu.chapter_download, popup.menu)

                // Hide download and show delete if the chapter is downloaded
                if (chapter.status != Download.DOWNLOADED) popup.menu.findItem(R.id.action_delete)
                    .title = download_button.context.getString(
                    R.string.action_cancel
                )

                // Set a listener so we are notified if a menu item is clicked
                popup.setOnMenuItemClickListener { _ ->
                    adapter.coverListener?.downloadChapter(adapterPosition)
                    true
                }

                // Finally show the PopupMenu
                popup.show()
            }
        }
        else {
            adapter.coverListener?.downloadChapter(adapterPosition)
        }
    }

    override fun bind(item: ChapterItem, manga: Manga) {
        val chapter = item.chapter
        val isLocked = item.isLocked
        chapter_title.text = when (manga.displayMode) {
            Manga.DISPLAY_NUMBER -> {
                val number = adapter.decimalFormat.format(chapter.chapter_number.toDouble())
                itemView.context.getString(R.string.display_mode_chapter, number)
            }
            else -> chapter.name
        }

        //chapter_menu.visible()
        // Set the correct drawable for dropdown and update the tint to match theme.
        //chapter_menu.setVectorCompat(R.drawable.ic_more_vert_black_24dp, view.context
           // .getResourceColor(R.attr.icon_color))

        if (isLocked) download_button.invisible()


        // Set correct text color
        chapter_title.setTextColor(if (chapter.read && !isLocked)
            adapter.readColor else adapter.unreadColor)
        if (chapter.bookmark && !isLocked) chapter_title.setTextColor(adapter.bookmarkedColor)

        val statuses = mutableListOf<String>()

        if (chapter.date_upload > 0) {
            statuses.add(DateUtils.getRelativeTimeSpanString(chapter.date_upload,
                Date().time, DateUtils.HOUR_IN_MILLIS).toString())
        }

        if (!chapter.read && chapter.last_page_read > 0 && chapter.pages_left > 0 && !isLocked) {
            statuses.add(itemView.resources.getQuantityString(R.plurals.pages_left, chapter
                .pages_left, chapter.pages_left))
        }
        else if (!chapter.read && chapter.last_page_read > 0 && !isLocked) {
            statuses.add(itemView.context.getString(R.string.chapter_progress, chapter
                .last_page_read + 1))
        }

        if (!chapter.scanlator.isNullOrBlank()) {
            statuses.add(chapter.scanlator!!)
        }

        chapter_scanlator.setTextColor(if (chapter.read) adapter.readColor else adapter.unreadColor)
        chapter_scanlator.text = statuses.joinToString(" • ")
        notifyStatus(item.status, item.isLocked, item.progress)
    }

    fun notifyStatus(status: Int, locked: Boolean, progress: Int) = with(download_button) {
        if (locked) {
            gone()
            return
        }
        visible()
        setDownoadStatus(status, progress)
    }
}
