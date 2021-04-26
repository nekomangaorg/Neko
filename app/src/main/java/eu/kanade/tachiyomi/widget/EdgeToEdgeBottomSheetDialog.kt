package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.updateLayoutParams

@Suppress("LeakingThis")
abstract class EdgeToEdgeBottomSheetDialog<VB : ViewBinding>(activity: Activity) :
    BottomSheetDialog(activity, R.style.BottomSheetDialogTheme) {
    protected val binding: VB

    protected val sheetBehavior: BottomSheetBehavior<*>

    init {
        binding = createBinding(activity.layoutInflater)
        setContentView(binding.root)

        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)

        val contentView = binding.root

        window?.setBackgroundDrawable(null)
        window?.navigationBarColor = activity.window.navigationBarColor
        val isLight = (activity.window?.decorView?.systemUiVisibility ?: 0) and View
            .SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLight) {
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window?.findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows =
            false
        window?.findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows =
            false
        contentView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View
            .SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val insets = activity.window.decorView.rootWindowInsets
        (contentView.parent as View).background = null
        contentView.post {
            (contentView.parent as View).background = null
        }
        contentView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.systemWindowInsetLeft
            rightMargin = insets.systemWindowInsetRight
        }
        contentView.requestLayout()
    }

    abstract fun createBinding(inflater: LayoutInflater): VB
}
