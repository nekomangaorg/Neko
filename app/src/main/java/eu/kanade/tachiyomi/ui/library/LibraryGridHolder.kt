package eu.kanade.tachiyomi.ui.library

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.updateLayoutParams
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
        with(title) {
            visibility = if (item.manga.hide_title) View.GONE else View.VISIBLE
            text = item.manga.currentTitle()
        }
        gradient.visibility = if (item.manga.hide_title) View.GONE else View.VISIBLE

        // Update the unread count and its visibility.
        with(unread_text) {
            visibility =
                if (item.manga.unread > 0 && item.unreadType == 1) View.VISIBLE else View.GONE
            text = item.manga.unread.toString()
        }
        // Update the download count and its visibility.
        unread_badge.visibility =
            if (item.manga.unread > 0 && item.unreadType == 0) View.VISIBLE else View.GONE
        with(download_text) {
            visibility = if (item.downloadCount > 0) View.VISIBLE else View.GONE
            text = item.downloadCount.toString()
        }
        //set local visibility if its local manga
        local_text.visibility = if (item.manga.source == LocalSource.ID) View.VISIBLE else View.GONE

        // Update the cover.
        if (item.manga.thumbnail_url == null)
            GlideApp.with(view.context).clear(thumbnail)
        else GlideApp.with(view.context).load(item.manga)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(item.manga.id!!).toString()))
            .centerCrop().into(thumbnail)
    }

}
