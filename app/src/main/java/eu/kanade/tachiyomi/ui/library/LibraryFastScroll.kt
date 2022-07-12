package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.ui.base.MaterialFastScroll

class LibraryFastScroll @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialFastScroll(context, attrs) {

    override fun setRecyclerViewPosition(y: Float) {
        if (recyclerView != null) {
            val adapter = recyclerView.adapter as? LibraryCategoryAdapter ?: return super.setRecyclerViewPosition(y)
            if (adapter.isSingleCategory) return super.setRecyclerViewPosition(y)
            var targetPos = getTargetPos(y)
            val category = when (val item = adapter.getItem(targetPos)) {
                is LibraryItem -> {
                    targetPos = adapter.currentItems.indexOf(item.header)
                    item.header.category
                }
                is LibraryHeaderItem -> item.category
                else -> return super.setRecyclerViewPosition(y)
            }
            adapter.controller?.scrollToCategory(category)
            updateBubbleText(targetPos)
        }
    }
}
