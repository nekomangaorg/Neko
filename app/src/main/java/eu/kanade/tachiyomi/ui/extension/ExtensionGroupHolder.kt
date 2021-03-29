package eu.kanade.tachiyomi.ui.extension

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.ExtensionCardHeaderBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder

class ExtensionGroupHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>) :
    BaseFlexibleViewHolder(view, adapter) {

    private val binding = ExtensionCardHeaderBinding.bind(view)

    @SuppressLint("SetTextI18n")
    fun bind(item: ExtensionGroupItem) {
        binding.title.text = item.name
    }
}
