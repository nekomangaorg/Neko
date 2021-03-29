package eu.kanade.tachiyomi.ui.migration

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.api.clear
import coil.api.loadAny
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.MangaListItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder

class MangaHolder(
    view: View,
    adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
) : BaseFlexibleViewHolder(view, adapter) {

    private val binding = MangaListItemBinding.bind(view)
    fun bind(item: MangaItem) {
        // Update the title of the manga.
        binding.title.text = item.manga.title
        binding.subtitle.text = ""

        // Update the cover.
        binding.coverThumbnail.clear()
        binding.coverThumbnail.loadAny(item.manga)
    }
}
