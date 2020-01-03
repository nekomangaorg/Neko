package eu.kanade.tachiyomi.ui.library

import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import cn.nekocode.badge.BadgeDrawable
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
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
        private val adapter: FlexibleAdapter<*>

) : LibraryHolder(view, adapter) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title of the manga.
        title.text = item.manga.title
        val unreadCount = (if (item.manga.unread > 0) item.manga.unread else "").toString()
        val downloadCount = (if (item.downloadCount > 0) item.downloadCount else "").toString()

        // Update the unread count and its visibility.
        with(unread_text) {
            visibility = if (item.manga.unread > 0 || item.downloadCount > 0) View.VISIBLE else View.GONE

            val badge = BadgeDrawable.Builder()
                    .type(BadgeDrawable.TYPE_WITH_TWO_TEXT_COMPLEMENTARY)
                    .badgeColor(ResourcesCompat.getColor(resources, R.color.colorPrimary, null))
                    .text1(unreadCount)
                    .text2(downloadCount)
                    .padding(10f, 10f, 10f, 10f, 10f)
                    .textSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, resources.displayMetrics))
                    .textColor(ResourcesCompat.getColor(resources, R.color.md_white_1000, null))
                    .build()
            text = badge.toSpannable()
        }

// Update the cover.
        GlideApp.with(view.context).clear(thumbnail)
        GlideApp.with(view.context)
                .load(item.manga)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .into(thumbnail)
    }

}
