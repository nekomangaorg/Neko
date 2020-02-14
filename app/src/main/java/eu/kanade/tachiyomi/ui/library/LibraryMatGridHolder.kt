package eu.kanade.tachiyomi.ui.library

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.android.synthetic.main.catalogue_mat_grid_item.*
import kotlinx.android.synthetic.main.catalogue_mat_grid_item.view.*

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_catalogue_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */
class LibraryMatGridHolder(
    private val view: View,
    adapter: LibraryCategoryAdapter,
    var width:Int,
    var fixedSize: Boolean
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

        // Update the subtitle of the manga with artist or the unread count
        with(subtitle) {
            text = when {
                item.manga.unread > 0 -> when (item.unreadType) {
                    1 -> view.resources.getQuantityString(R.plurals.unread_count, unread, unread)
                    0 -> view.resources.getString(R.string.new_chapter)
                    else -> item.manga.originalAuthor()
                }
                else -> item.manga.originalAuthor()
            }
            setTextColor(
                view.context.getResourceColor(
                    if (item.manga.unread > 0 && item.unreadType > -1) android.R.attr.colorAccent
                    else android.R.attr.textColorSecondary
                )
            )
        }
        play_layout.visibility = if (unread > 0) View.VISIBLE else View.GONE
        play_layout.setOnClickListener { playButtonClicked() }

        // Update the download count and its visibility.
        with(download_text) {
            visibility = if (item.downloadCount > 0) View.VISIBLE else View.GONE
            text = item.downloadCount.toString()
        }
        // Set local visibility if its local manga
        local_text.visibility = if (item.manga.source == LocalSource.ID) View.VISIBLE else View.GONE

        // Update the cover.
        if (item.manga.thumbnail_url == null) Glide.with(view.context).clear(cover_thumbnail)
        else {
            val id = item.manga.id ?: return
            var glide = GlideApp.with(view.context).load(item.manga)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .signature(ObjectKey(MangaImpl.getLastCoverFetch(id).toString()))
            glide = if (fixedSize) glide.centerCrop() else glide.override(width)
            glide.into(cover_thumbnail)
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
