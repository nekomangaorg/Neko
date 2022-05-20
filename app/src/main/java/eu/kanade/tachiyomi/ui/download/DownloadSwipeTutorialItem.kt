package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadSwipeTutItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder

class DownloadSwipeTutorialItem : AbstractFlexibleItem<DownloadSwipeTutorialItem.Holder>() {

    /**
     * Returns the layout resource for this item.
     */
    override fun getLayoutRes(): Int {
        return R.layout.download_swipe_tut_item
    }

    /**
     * Returns a new view holder for this item.
     *
     * @param view The view of this item.
     * @param adapter The adapter of this item.
     */
    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): Holder {
        return Holder(view, adapter)
    }

    /**
     * Binds the given view holder with this item.
     *
     * @param adapter The adapter of this item.
     * @param holder The holder to bind.
     * @param position The position of this item in the adapter.
     * @param payloads List of partial changes.
     */
    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
    }

    /**
     * Returns true if this item is draggable.
     */
    override fun isDraggable(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DownloadSwipeTutorialItem
    }

    override fun hashCode(): Int {
        return "Download Tut".hashCode()
    }

    class Holder(val view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>) :
        BaseFlexibleViewHolder(view, adapter, true) {

        private val binding = DownloadSwipeTutItemBinding.bind(view)

        override fun getFrontView(): View {
            return binding.frontView
        }

        override fun getRearStartView(): View {
            return binding.startView
        }

        override fun getRearEndView(): View {
            return binding.endView
        }
    }
}
