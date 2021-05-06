package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.view.isVisible
import com.google.android.material.appbar.MaterialToolbar
import eu.kanade.tachiyomi.R

open class BaseToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialToolbar(context, attrs) {

    protected lateinit var toolbarTitle: TextView
    private val defStyleRes = com.google.android.material.R.style.Widget_MaterialComponents_Toolbar

    protected val titleTextAppearance: Int

    var incognito = false
    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.Toolbar,
            0,
            defStyleRes
        )
        titleTextAppearance = a.getResourceId(R.styleable.Toolbar_titleTextAppearance, 0)
        a.recycle()
    }

    override fun setTitle(resId: Int) {
        setCustomTitle(context.getString(resId))
    }

    override fun setTitle(title: CharSequence?) {
        setCustomTitle(title)
    }

    protected open fun setCustomTitle(title: CharSequence?) {
        toolbarTitle.isVisible = true
        toolbarTitle.text = title
        super.setTitle(null)
        setIncognitoMode(incognito)
    }

    fun setIncognitoMode(enabled: Boolean) {
        incognito = enabled
        setIcons()
    }

    open fun setIcons() {
        toolbarTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
            getIncogRes(),
            0,
            getDropdownRes(),
            0
        )
    }

    @DrawableRes
    private fun getIncogRes(): Int {
        return when {
            incognito -> R.drawable.ic_incognito_circle_24dp
            else -> 0
        }
    }

    @DrawableRes
    private fun getDropdownRes(): Int {
        return when {
            incognito && navigationIcon !is DrawerArrowDrawable -> R.drawable.ic_blank_28dp
            else -> 0
        }
    }
}
