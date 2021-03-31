package eu.kanade.tachiyomi.ui.extension

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.RecyclerWithScrollerBinding

class RecyclerWithScrollerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    var binding: RecyclerWithScrollerBinding? = null
    fun setUp(sheet: ExtensionBottomSheet, binding: RecyclerWithScrollerBinding) {
        binding.recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.addItemDecoration(ExtensionDividerItemDecoration(context))
        this.binding = binding
    }

    fun onBind(adapter: FlexibleAdapter<IFlexible<*>>) {
        binding?.recycler?.adapter = adapter
        adapter.fastScroller = binding?.fastScroller
    }
}
