package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import kotlinx.android.synthetic.main.tabbed_bottom_sheet.*


abstract class TabbedBottomSheetDialog(private val activity: Activity) :
    BottomSheetDialog
        (activity, R.style.BottomSheetDialogTheme) {

    private var sheetBehavior: BottomSheetBehavior<*>

    open var offset = -1
    init {
        // Use activity theme for this layout
        val view = activity.layoutInflater.inflate(R.layout.tabbed_bottom_sheet, null)

        setContentView(view)
        sheetBehavior = BottomSheetBehavior.from(view.parent as ViewGroup)
        setEdgeToEdge(activity, view)

        val height = activity.window.decorView.rootWindowInsets.systemWindowInsetTop
        pager.maxHeight = activity.window.decorView.height  - height - 125.dpToPx

        val adapter = TabbedSheetAdapter()
        pager.offscreenPageLimit = 2
        pager.adapter = adapter
        tabs.setupWithViewPager(pager)
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
        sheetBehavior.expand()
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

class MeasuredViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null): ViewPager(context, attrs) {

    var maxHeight = 0
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var height = 0
        val childWidthSpec = MeasureSpec.makeMeasureSpec(
            Math.max(
                0, MeasureSpec.getSize(widthMeasureSpec) -
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
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY) + (rootWindowInsets?.systemWindowInsetBottom ?: 0)
        }
        if (maxHeight < height) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST) + (rootWindowInsets?.systemWindowInsetBottom ?: 0)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}