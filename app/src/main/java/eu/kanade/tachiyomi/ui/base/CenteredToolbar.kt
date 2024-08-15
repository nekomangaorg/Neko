package eu.kanade.tachiyomi.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.view.updateLayoutParams
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.util.system.contextCompatDrawable
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import org.nekomanga.R

@SuppressLint("CustomViewStyleable")
class CenteredToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseToolbar(context, attrs) {

    override fun onFinishInflate() {
        super.onFinishInflate()
        toolbarTitle = findViewById<MaterialTextView>(R.id.toolbar_title)
        toolbarTitle.setTextAppearance(titleTextAppearance)
        toolbarTitle.setTextColor(context.getResourceColor(R.attr.actionBarTintColor))
        collapseIcon =
            context.contextCompatDrawable(R.drawable.ic_arrow_back_24dp)?.apply {
                setTint(context.getResourceColor(R.attr.actionBarTintColor))
            }
    }

    override fun setCustomTitle(title: CharSequence?) {
        super.setCustomTitle(title)
        toolbarTitle.updateLayoutParams<LayoutParams> {
            gravity = if (!onRoot) Gravity.START else Gravity.CENTER
        }
        toolbarTitle.compoundDrawablePadding = if (!onRoot) 6.dpToPx else 0
    }
}
