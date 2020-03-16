package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.main_activity.view.*

class CenteredToolbar@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : Toolbar
    (context, attrs) {

    override fun setTitle(resId: Int) {
        if (navigationIcon is DrawerArrowDrawable) {
            super.setTitle(resId)
            toolbar_title.text = null
        } else {
            toolbar_title.text = context.getString(resId)
            post {
                if (navigationIcon !is DrawerArrowDrawable) {
                    toolbar_title.text = context.getString(resId)
                    requestLayout()
                }
            }
            super.setTitle(null)
        }
    }

    override fun setTitle(title: CharSequence?) {
        if (navigationIcon is DrawerArrowDrawable) {
            super.setTitle(title)
            toolbar_title.text = ""
        } else {
            toolbar_title.text = title
            post {
                if (navigationIcon !is DrawerArrowDrawable) {
                    toolbar_title.text = title
                    requestLayout()
                }
            }
            super.setTitle(null)
        }
    }

    fun showSpinner(): PopupMenu {
        val popupMenu = PopupMenu(context, title_layout, Gravity.CENTER)
        dropdown.visible()
        title_layout.setOnTouchListener(popupMenu.dragToOpenListener)
        title_layout.setOnClickListener {
            popupMenu.show()
        }
        return popupMenu
    }

    fun removeSpinner() {
        dropdown.gone()
        title_layout.setOnTouchListener(null)
        title_layout.setOnClickListener(null)
    }
}
