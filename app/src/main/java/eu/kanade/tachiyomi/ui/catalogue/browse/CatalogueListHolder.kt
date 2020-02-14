package eu.kanade.tachiyomi.ui.catalogue.browse

import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.widget.StateImageViewTarget
import kotlinx.android.synthetic.main.catalogue_list_item.*

/**
 * Class used to hold the displayed data of a manga in the catalogue, like the cover or the title.
 * All the elements from the layout file "item_catalogue_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new catalogue holder.
 */
class CatalogueListHolder(private val view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>) :
        CatalogueHolder(view, adapter) {

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        title.text = manga.originalTitle()
        with(subtitle) {
            visibility = if (manga.favorite) View.VISIBLE else View.GONE
            text = view.resources.getString(R.string.in_library)
            setTextColor(view.context.getResourceColor(android.R.attr.colorAccent))
        }

        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        if (manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(view.context).clear(contentView)
        } else {
            GlideApp.with(view.context)
                .load(manga)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .dontAnimate()
                .placeholder(android.R.color.transparent)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(StateImageViewTarget(cover_thumbnail, progress))
        }
    }

}
