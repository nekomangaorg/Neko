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
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import kotlinx.android.synthetic.main.catalogue_grid_item.*
import kotlinx.android.synthetic.main.catalogue_list_item.badge_view
import kotlinx.android.synthetic.main.catalogue_list_item.cover_thumbnail
import kotlinx.android.synthetic.main.catalogue_list_item.download_text
import kotlinx.android.synthetic.main.catalogue_list_item.play_layout
import kotlinx.android.synthetic.main.catalogue_list_item.subtitle
import kotlinx.android.synthetic.main.catalogue_list_item.title
import kotlinx.android.synthetic.main.catalogue_list_item.unread_angle
import kotlinx.android.synthetic.main.catalogue_list_item.unread_text
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

        with(unread_text) {
            text = unread.toString()
            visibility = if (unread > 0) View.VISIBLE else View.GONE
        }

        // Update the download count or local status and its visibility.
        with(download_text) {
            visibility = if (item.downloadCount > -1 && (item.downloadCount > 0 || item.manga
                    .source == LocalSource.ID))
                View.VISIBLE else View.GONE
            text = if (item.manga.source == LocalSource.ID)
                itemView.resources.getString(R.string.local_source_badge)
            else item.downloadCount.toString()
        }

        // Show the bade card if unread or downloads exists
        badge_view.visibility = if (download_text.visibility == View.VISIBLE || unread_text
                .visibility == View.VISIBLE) View.VISIBLE else View.GONE

        // Show the angles divider if both unread and downloads exists
        unread_angle.visibility = if (download_text.visibility == View.VISIBLE && unread_text
                .visibility == View.VISIBLE) View.VISIBLE else View.GONE

        if (unread_angle.visibility == View.VISIBLE) {
            download_text.updatePaddingRelative(end = 8.dpToPx)
            unread_text.updatePaddingRelative(start = 2.dpToPx)
        }
        else {
            download_text.updatePaddingRelative(end = 5.dpToPx)
            unread_text.updatePaddingRelative(start = 5.dpToPx)
        }

        /* if (item.downloadCount > 0 || item.manga.source == LocalSource.ID) {
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
        }*/
        /*else {
            subtitleText
        }*/
        subtitle.text = item.manga.originalAuthor()?.trim()
        subtitle.visibility = if (!item.manga.originalAuthor().isNullOrBlank()) View.VISIBLE
        else View.GONE

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
