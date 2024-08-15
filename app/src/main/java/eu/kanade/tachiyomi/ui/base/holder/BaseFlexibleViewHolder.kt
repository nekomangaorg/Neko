package eu.kanade.tachiyomi.ui.base.holder

import android.view.View
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.viewholders.FlexibleViewHolder

abstract class BaseFlexibleViewHolder(
    view: View,
    adapter: FlexibleAdapter<*>,
    stickyHeader: Boolean = false,
) : FlexibleViewHolder(view, adapter, stickyHeader) {
    override fun getRearRightView(): View? {
        return getRearEndView()
    }

    override fun getRearLeftView(): View? {
        return getRearStartView()
    }

    open fun getRearStartView(): View? {
        return null
    }

    open fun getRearEndView(): View? {
        return null
    }
}
