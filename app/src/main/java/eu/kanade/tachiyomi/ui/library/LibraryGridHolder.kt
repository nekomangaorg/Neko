package eu.kanade.tachiyomi.ui.library

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.util.view.gone
import kotlinx.android.synthetic.main.catalogue_grid_item.*
import kotlinx.android.synthetic.main.unread_download_badge.*

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_catalogue_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */
class LibraryGridHolder(
    private val view: View,
    adapter: LibraryCategoryAdapter,
    var width:Int,
    compact: Boolean,
    private var fixedSize: Boolean
) : LibraryHolder(view, adapter) {

    init {
        play_layout.setOnClickListener { playButtonClicked() }
        if (compact) {
            text_layout.gone()
        }
        else {
            compact_title.gone()
            gradient.gone()
            val playLayout = play_layout.layoutParams as FrameLayout.LayoutParams
            val buttonLayout = play_button.layoutParams as FrameLayout.LayoutParams
            playLayout.gravity = Gravity.BOTTOM or Gravity.END
            buttonLayout.gravity = Gravity.BOTTOM or Gravity.END
            play_layout.layoutParams = playLayout
            play_button.layoutParams = buttonLayout
        }
    }

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title and subtitle of the manga.
        title.text = item.manga.currentTitle()
        subtitle.text = item.manga.originalAuthor()?.trim()

        compact_title.text = title.text

        setUnreadBadge(badge_view, item)
        play_layout.visibility = if (item.manga.unread > 0 && item.unreadType > 0)
            View.VISIBLE else View.GONE

        // Update the cover.
        if (item.manga.thumbnail_url == null) GlideApp.with(view.context).clear(cover_thumbnail)
        else {
            val id = item.manga.id ?: return
            if (cover_thumbnail.height == 0) {
                val oldPos = adapterPosition
                adapter.recyclerView.post {
                    if (oldPos == adapterPosition)
                        setCover(item.manga, id)
                }
            }
            else setCover(item.manga, id)
        }
    }

    private fun setCover(manga: Manga, id: Long) {
        GlideApp.with(adapter.recyclerView.context).load(manga)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(id).toString()))
            .apply {
                if (fixedSize) centerCrop()
                else override(cover_thumbnail.maxHeight)
            }
            .into(cover_thumbnail)
    }

    private fun playButtonClicked() {
        adapter.libraryListener.startReading(adapterPosition)
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == 2) {
            card.isDragged = true
            badge_view.isDragged = true
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        card.isDragged = false
        badge_view.isDragged = false
    }

}
