package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Edge to Edge BottomSheetDiolag that uses a custom theme and settings to extend pass the nav bar
 */
@Suppress("LeakingThis")
abstract class E2EBottomSheetDialog<VB : ViewBinding>(activity: Activity) :
    BottomSheetDialog(activity) {
    protected val binding: VB

    protected val sheetBehavior: BottomSheetBehavior<*>
    protected open var recyclerView: RecyclerView? = null

    private val isLight: Boolean
    init {
        binding = createBinding(activity.layoutInflater)
        setContentView(binding.root)

        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)

        val contentView = binding.root

        val aWic = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        isLight = aWic.isAppearanceLightStatusBars
        window?.let { window ->
            val wic = WindowInsetsControllerCompat(window, binding.root)
            window.navigationBarColor = activity.window.navigationBarColor
            wic.isAppearanceLightNavigationBars = isLight
        }
        (contentView.parent as View).background = null
        contentView.post {
            (contentView.parent as View).background = null
        }
        contentView.requestLayout()
    }

    override fun onStart() {
        super.onStart()
        recyclerView?.let { recyclerView ->
            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE ||
                            newState == RecyclerView.SCROLL_STATE_SETTLING
                        ) {
                            sheetBehavior.isDraggable = true
                        } else {
                            sheetBehavior.isDraggable = !recyclerView.canScrollVertically(-1)
                        }
                    }
                },
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window?.let { window ->
            val wic = WindowInsetsControllerCompat(window, binding.root)
            wic.isAppearanceLightNavigationBars = isLight
        }
    }

    abstract fun createBinding(inflater: LayoutInflater): VB
}
