package eu.kanade.tachiyomi.ui.manga.merge

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.clear
import coil.load
import coil.transform.RoundedCornersTransformation
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MergeSearchItemBinding
import eu.kanade.tachiyomi.source.model.SManga

class MergeSearchItem(val manga: SManga) : AbstractBindingItem<MergeSearchItemBinding>() {
    override val type: Int = R.id.merge_search_item

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
    ): MergeSearchItemBinding {
        return MergeSearchItemBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: MergeSearchItemBinding, payloads: List<Any>) {
        binding.mergeSearchTitle.text = manga.title
        binding.mergeSearchCover.clear()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            binding.mergeSearchCover.load(manga.thumbnail_url) {
                transformations(RoundedCornersTransformation(2f))
            }
        }
    }

    override fun unbindView(binding: MergeSearchItemBinding) {
        binding.mergeSearchCover.clear()
    }
}
