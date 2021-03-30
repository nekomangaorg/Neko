package eu.kanade.tachiyomi.ui.source.global_search

import android.view.View
import coil.Coil
import coil.api.clear
import coil.request.CachePolicy
import coil.request.LoadRequest
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.view.visibleIf
import eu.kanade.tachiyomi.data.image.coil.CoverViewTarget
import eu.kanade.tachiyomi.databinding.SourceGlobalSearchControllerCardItemBinding

class GlobalSearchMangaHolder(view: View, adapter: GlobalSearchCardAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    private val binding = SourceGlobalSearchControllerCardItemBinding.bind(view)
    init {
        // Call onMangaClickListener when item is pressed.
        itemView.setOnClickListener {
            val item = adapter.getItem(flexibleAdapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaClick(item.manga)
            }
        }
        itemView.setOnLongClickListener {
            val item = adapter.getItem(flexibleAdapterPosition)
            if (item != null) {
                adapter.mangaClickListener.onMangaLongClick(item.manga)
            }
            true
        }
    }

    fun bind(manga: Manga) {
        binding.title.text = manga.title
        binding.favoriteButton.visibleIf(manga.favorite)
        setImage(manga)
    }

    fun setImage(manga: Manga) {
        binding.itemImage.clear()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            val request = LoadRequest.Builder(itemView.context).data(manga)
                .placeholder(android.R.color.transparent)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .target(CoverViewTarget(binding.itemImage, binding.progress)).build()
            Coil.imageLoader(itemView.context).execute(request)
        }
    }
}
