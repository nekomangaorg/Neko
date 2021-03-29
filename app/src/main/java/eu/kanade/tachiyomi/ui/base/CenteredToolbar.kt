package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.R

class CenteredToolbar@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialToolbar(context, attrs) {

    private lateinit var toolbarTitle: TextView
    override fun onFinishInflate() {
        super.onFinishInflate()
        toolbarTitle = findViewById<MaterialTextView>(R.id.toolbar_title)
    }

    override fun setTitle(resId: Int) {
        if (navigationIcon is DrawerArrowDrawable) {
            super.setTitle(resId)
            toolbarTitle.text = null
            hideDropdown()
        } else {
            toolbarTitle.text = context.getString(resId)
            super.setTitle(null)
        }
    }

    override fun setTitle(title: CharSequence?) {
        if (navigationIcon is DrawerArrowDrawable) {
            super.setTitle(title)
            toolbarTitle.text = ""
            hideDropdown()
        } else {
            toolbarTitle.text = title
            super.setTitle(null)
        }
    }

    fun showDropdown(down: Boolean = true) {
        toolbarTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_blank_24dp,
            0,
            if (down) {
                R.drawable.ic_arrow_drop_down_24dp
            } else {
                R.drawable.ic_arrow_drop_up_24dp
            },
            0
        )
    }

    fun hideDropdown() {
        toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}
