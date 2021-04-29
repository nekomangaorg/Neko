package eu.kanade.tachiyomi.ui.extension

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.RecyclerWithScrollerBinding
import eu.kanade.tachiyomi.util.view.updatePaddingRelative

class RecyclerWithScrollerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    var binding: RecyclerWithScrollerBinding? = null
    fun setUp(sheet: ExtensionBottomSheet, binding: RecyclerWithScrollerBinding, height: Int) {
        binding.recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.addItemDecoration(ExtensionDividerItemDecoration(context))
        binding.recycler.updatePaddingRelative(bottom = height)
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE ||
                    newState == RecyclerView.SCROLL_STATE_SETTLING
                ) {
                    sheet.sheetBehavior?.isDraggable = true
                } else {
                    sheet.sheetBehavior?.isDraggable = !recyclerView.canScrollVertically(-1)
                }
            }
        })

        this.binding = binding
    }

    fun onBind(adapter: FlexibleAdapter<IFlexible<*>>) {
        binding?.recycler?.adapter = adapter
        adapter.fastScroller = binding?.fastScroller
    }
}
