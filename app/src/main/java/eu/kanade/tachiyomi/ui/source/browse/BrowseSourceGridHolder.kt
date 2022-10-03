package eu.kanade.tachiyomi.ui.source.browse

import android.app.Activity
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.dispose
import coil.request.ImageRequest
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.CoverViewTarget
import eu.kanade.tachiyomi.databinding.MangaGridItemBinding
import eu.kanade.tachiyomi.ui.library.LibraryCategoryAdapter
import eu.kanade.tachiyomi.util.view.setCards

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_catalogue_grid" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */
class BrowseSourceGridHolder(
    private val view: View,
    private val adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    compact: Boolean,
    showOutline: Boolean,
    private val isFollows: Boolean = false,
) : BrowseSourceHolder(view, adapter) {

    private val binding = MangaGridItemBinding.bind(view)

    init {
        if (compact) {
            binding.textLayout.isVisible = false
        } else {
            binding.compactTitle.isVisible = false
            binding.gradient.isVisible = false
        }
        setCards(showOutline, binding.card, binding.unreadDownloadBadge.badgeView)
    }

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga item to bind.
     */
    override fun onSetValues(manga: Manga) {
        // Update the title of the manga.
        binding.title.text = manga.title
        binding.compactTitle.text = binding.title.text
        when (isFollows) {
            true -> binding.unreadDownloadBadge.root.setStatus(
                manga.follow_status!!,
                manga.favorite,
            )
            false -> binding.unreadDownloadBadge.root.setInLibrary(manga.favorite)
        }

        // Update the cover.
        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        if ((view.context as? Activity)?.isDestroyed == true) return
        if (manga.thumbnail_url == null) {
            binding.coverThumbnail.dispose()
        } else {
            manga.id ?: return
            val request = ImageRequest.Builder(view.context).data(manga)
                .target(CoverViewTarget(binding.coverThumbnail, binding.progress))
                .build()
            Coil.imageLoader(view.context).enqueue(request)
        }
    }
}
