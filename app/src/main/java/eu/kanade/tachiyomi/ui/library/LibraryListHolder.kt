package eu.kanade.tachiyomi.ui.library

import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import kotlinx.android.synthetic.main.catalogue_list_item.*
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.android.synthetic.main.catalogue_list_item.subtitle
import kotlinx.android.synthetic.main.catalogue_list_item.title
import kotlinx.android.synthetic.main.catalogue_list_item.view.*

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_library_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */

class LibraryListHolder(
        private val view: View,
        adapter: LibraryCategoryAdapter
) : LibraryHolder(view, adapter) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title of the manga.
        title.text = item.manga.currentTitle()

        // Update the unread count and its visibility.
        val unread = item.manga.unread

        // Update the subtitle of the manga with artist or the unread count and download count
        "<font color=#cc0029>First Color</font>"
        val subtitleText = when {
            unread > 0 -> when (item.unreadType) {
                1 -> view.resources.getQuantityString(R.plurals.unread_count, unread, unread)
                0 -> view.resources.getString(R.string.new_chapter)
                else -> item.manga.originalAuthor()
            }
            else -> item.manga.originalAuthor()
        }
        // Update the download count or local status and its visibility.
        val downloadText =
            if (item.manga.source == LocalSource.ID)
                itemView.resources.getString(R.string.local_source_badge)
            else view.resources.getQuantityString(R.plurals.download_count,
                item.downloadCount, item.downloadCount)

        // Combine the 2 above using html
        val subText = if (item.downloadCount > 0 || item.manga.source == LocalSource.ID) {
            val downloadColor = convertColor(ContextCompat.getColor(itemView.context,
                if (item.manga.source == LocalSource.ID) R.color.md_teal_500
                else R.color.md_red_500))
            val unreadColor = convertColor(itemView.context.getResourceColor(R.attr.colorAccent))
            when {
                unread > 0 && item.unreadType > -1 -> "<font color=" +
                    "#$downloadColor>$downloadText</font> | " +
                    "<font color=#$unreadColor>$subtitleText</font>"
                subtitleText != null -> "<font color=#$downloadColor>$downloadText</font>  |  " +
                    subtitleText
                else ->  "<font color=#$downloadColor>$downloadText</font>"
            }
        }
        else {
            subtitleText
        }
        with(subtitle) {
            text = HtmlCompat.fromHtml(subText ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
            setTextColor(
                view.context.getResourceColor(
                    if (item.manga.unread > 0 && item.unreadType > -1 && item.downloadCount <= 0
                        && item.manga.source != LocalSource.ID)
                        android.R.attr.colorAccent
                    else android.R.attr.textColorSecondary
                )
            )
        }
        play_layout.visibility = if (unread > 0) View.VISIBLE else View.GONE
        play_layout.setOnClickListener { playButtonClicked() }

        // Update the cover.
        if (item.manga.thumbnail_url == null) Glide.with(view.context).clear(cover_thumbnail)
        else {
            val id = item.manga.id ?: return
            val height = itemView.context.resources.getDimensionPixelSize(R.dimen
                .material_component_lists_single_line_with_avatar_height)
            GlideApp.with(view.context).load(item.manga)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .signature(ObjectKey(MangaImpl.getLastCoverFetch(id).toString()))
                .override(height)
                .into(cover_thumbnail)
        }
    }

    private fun convertColor(color: Int):String {
        return Integer.toHexString(color and 0x00ffffff)
    }

    private fun playButtonClicked() {
        adapter.libraryListener.startReading(adapterPosition)
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == 2) {
            view.card.isDragged = true
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        view.card.isDragged = false
    }

}
