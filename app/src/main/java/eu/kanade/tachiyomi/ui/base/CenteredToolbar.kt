package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.main_activity.view.*

class CenteredToolbar@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialToolbar(context, attrs) {

    override fun setTitle(resId: Int) {
        if (navigationIcon is DrawerArrowDrawable) {
            super.setTitle(resId)
            toolbar_title.text = null
        } else {
            toolbar_title.text = context.getString(resId)
            super.setTitle(null)
        }
    }

    override fun setTitle(title: CharSequence?) {
        if (navigationIcon is DrawerArrowDrawable) {
            super.setTitle(title)
            toolbar_title.text = ""
        } else {
            toolbar_title.text = title
            super.setTitle(null)
        }
    }
}
