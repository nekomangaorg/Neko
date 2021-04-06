package eu.kanade.tachiyomi.ui.recents

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.RecentsHeaderItemBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.library.LibraryHeaderItem
import eu.kanade.tachiyomi.util.view.visibleIf

class RecentMangaHeaderItem(val recentsType: Int) :
    AbstractHeaderItem<RecentMangaHeaderItem.Holder>() {

    override fun getLayoutRes(): Int {
        return R.layout.recents_header_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): Holder {
        return Holder(view, adapter as RecentMangaAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(recentsType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is LibraryHeaderItem) {
            return recentsType == recentsType
        }
        return false
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun isSwipeable(): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return recentsType.hashCode()
    }

    class Holder(val view: View, adapter: RecentMangaAdapter) : BaseFlexibleViewHolder(
        view,
        adapter,
        true
    ) {

        private val binding = RecentsHeaderItemBinding.bind(view)
        init {
            listOf(R.string.grouped, R.string.all, R.string.history, R.string.updates).forEach { resId ->
                binding.recentsTabs.addTab(binding.recentsTabs.newTab().setText(resId))
            }
            val selectedTab = (this@Holder.bindingAdapter as? RecentMangaAdapter)?.delegate?.getViewType() ?: 0
            binding.recentsTabs.selectTab(binding.recentsTabs.getTabAt(selectedTab))
            binding.recentsTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    (this@Holder.bindingAdapter as? RecentMangaAdapter)?.delegate?.setViewType(tab?.position ?: 0)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) { }
                override fun onTabReselected(tab: TabLayout.Tab?) { }
            })
        }

        fun bind(recentsType: Int) {
            binding.title.setText(
                when (recentsType) {
                    CONTINUE_READING -> R.string.continue_reading
                    NEW_CHAPTERS -> R.string.new_chapters
                    NEWLY_ADDED -> R.string.newly_added
                    else -> R.string.continue_reading
                }
            )
            binding.recentsTabs.visibleIf(recentsType == -1)
            val selectedTab = (this@Holder.bindingAdapter as? RecentMangaAdapter)?.delegate?.getViewType() ?: 0
            binding.recentsTabs.selectTab(binding.recentsTabs.getTabAt(selectedTab))
            binding.title.visibleIf(recentsType != -1)
        }
    }

    companion object {
        const val CONTINUE_READING = 0
        const val NEW_CHAPTERS = 1
        const val NEWLY_ADDED = 2
    }
}
