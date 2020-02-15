package eu.kanade.tachiyomi.ui.library

import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.catalogue_grid_item.*

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
    var fixedSize: Boolean
) : LibraryHolder(view, adapter) {

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
        if (!fixedSize) {
            title.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    title.viewTreeObserver.removeOnPreDrawListener(this)
                    // Drawing happens after layout so we can assume getLineCount() returns the correct value
                    val marginParams = title.layoutParams as ConstraintLayout.LayoutParams
                    if (title.lineCount == 2) {
                        // Do whatever you want in case text view has more than 2 lines
                        subtitle.gone()
                        marginParams.bottomMargin = 10.dpToPx
                        marginParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    } else {
                        subtitle.visible()
                        marginParams.bottomMargin = 0
                        marginParams.bottomToBottom = -1
                    }
                    title.layoutParams = marginParams
                    return true
                }
            })
        }

        compact_title.text = title.text

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

        play_layout.visibility = if (unread > 0) View.VISIBLE else View.GONE
        play_layout.setOnClickListener { playButtonClicked() }

        if (fixedSize) {
            title.gone()
            subtitle.gone()
        }
        else {
            compact_title.gone()
            gradient.gone()
        }

        // Update the cover.
        if (item.manga.thumbnail_url == null) GlideApp.with(view.context).clear(cover_thumbnail)
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
