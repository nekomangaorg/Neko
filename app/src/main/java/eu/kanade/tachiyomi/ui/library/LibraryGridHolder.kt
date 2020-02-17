package eu.kanade.tachiyomi.ui.library

import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
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
    private var fixedSize: Boolean
) : LibraryHolder(view, adapter) {

    init {
        play_layout.setOnClickListener { playButtonClicked() }
        if (fixedSize) {
            title.gone()
            subtitle.gone()
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

        badge_view.setUnreadDownload(
            when (item.unreadType) {
                1 -> item.manga.unread
                0 -> if (item.manga.unread > 0) -1 else -2
                else -> -2
            },
            when {
                item.downloadCount == -1 -> -1
                item.manga.source == LocalSource.ID -> -2
                else ->  item.downloadCount
            })
        play_layout.visibility = if (item.manga.unread > 0 && item.unreadType > -1)
            View.VISIBLE else View.GONE

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
