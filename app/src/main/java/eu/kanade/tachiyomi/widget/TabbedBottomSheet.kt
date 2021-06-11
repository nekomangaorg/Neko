package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.databinding.TabbedBottomSheetBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.expand
import kotlin.math.max

abstract class TabbedBottomSheetDialog(private val activity: Activity) :
    E2EBottomSheetDialog<TabbedBottomSheetBinding>(activity) {

    override fun createBinding(inflater: LayoutInflater) =
        TabbedBottomSheetBinding.inflate(inflater)

    open var offset = -1

    init {
        val height = activity.window.decorView.rootWindowInsets.systemWindowInsetTop
        binding.pager.maxHeight = activity.window.decorView.height - height - 125.dpToPx

        val adapter = TabbedSheetAdapter()
        binding.pager.offscreenPageLimit = 2
        binding.pager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.pager)
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
        sheetBehavior.expand()
        getTabViews().forEachIndexed { index, nestedScrollView ->
            val view = nestedScrollView as? NestedScrollView
            view?.isNestedScrollingEnabled = binding.pager.currentItem == index
            view?.requestLayout()
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val view = getTabViews()[tab?.position ?: 0] as? NestedScrollView
                view?.isNestedScrollingEnabled = true
                view?.requestLayout()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val view = getTabViews()[tab?.position ?: 0] as? NestedScrollView
                view?.isNestedScrollingEnabled = false
                view?.requestLayout()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val view = getTabViews()[tab?.position ?: 0] as? NestedScrollView
                view?.isNestedScrollingEnabled = true
                view?.requestLayout()
            }
        })
    }

    abstract fun getTabViews(): List<View>

    abstract fun getTabTitles(): List<Int>

    private inner class TabbedSheetAdapter : ViewPagerAdapter() {

        override fun createView(container: ViewGroup, position: Int): View {
            return getTabViews()[position]
        }

        override fun getCount(): Int {
            return getTabViews().size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return activity.resources!!.getString(getTabTitles()[position])
        }
    }
}

class MeasuredViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ViewPager(context, attrs) {

    var maxHeight = 0
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightSpec = heightMeasureSpec
        super.onMeasure(widthMeasureSpec, heightSpec)
        var height = 0
        val childWidthSpec = MeasureSpec.makeMeasureSpec(
            max(
                0,
                MeasureSpec.getSize(widthMeasureSpec) -
                    paddingLeft - paddingRight
            ),
            MeasureSpec.getMode(widthMeasureSpec)
        )
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(childWidthSpec, MeasureSpec.UNSPECIFIED)
            val h = child.measuredHeight
            if (h > height) height = h
        }
        if (height != 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }
        if (maxHeight < height + (rootWindowInsets?.systemWindowInsetBottom ?: 0)) {
            heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
