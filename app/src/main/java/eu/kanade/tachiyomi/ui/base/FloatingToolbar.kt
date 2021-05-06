package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.contextCompatDrawable
import eu.kanade.tachiyomi.util.system.getResourceColor

class FloatingToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseToolbar(context, attrs) {

    private val actionColorAlpha = ColorUtils.setAlphaComponent(context.getResourceColor(R.attr.actionBarTintColor), 200)
    private val actionColorAlphaSecondary = ColorUtils.setAlphaComponent(context.getResourceColor(R.attr.actionBarTintColor), 150)

    private lateinit var toolbarsubTitle: TextView
    private lateinit var cardIncogImage: ImageView
    private val defStyleRes = com.google.android.material.R.style.Widget_MaterialComponents_Toolbar
    private val subtitleTextAppeance: Int

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.Toolbar,
            0,
            defStyleRes
        )
        subtitleTextAppeance = a.getResourceId(R.styleable.Toolbar_subtitleTextAppearance, 0)
        a.recycle()
    }
    override fun onFinishInflate() {
        super.onFinishInflate()
        toolbarTitle = findViewById<MaterialTextView>(R.id.card_title)
        toolbarTitle.setTextAppearance(titleTextAppearance)
        toolbarTitle.setTextColor(actionColorAlpha)

        toolbarsubTitle = findViewById<MaterialTextView>(R.id.card_subtitle)
        toolbarsubTitle.setTextAppearance(subtitleTextAppeance)
        toolbarsubTitle.setTextColor(actionColorAlphaSecondary)
        toolbarsubTitle.isVisible = false

        cardIncogImage = findViewById(R.id.card_incog_image)

        setNavigationIconTint(actionColorAlpha)
        collapseIcon = context.contextCompatDrawable(R.drawable.ic_arrow_back_24dp)?.apply {
            setTint(actionColorAlpha)
        }
    }

    override fun setSubtitle(resId: Int) {
        setCustomSubtitle(context.getString(resId))
    }

    override fun setSubtitle(subtitle: CharSequence?) {
        setCustomSubtitle(subtitle)
    }

    override fun setIcons() {
        cardIncogImage.isVisible = incognito
    }

    private fun setCustomSubtitle(title: CharSequence?) {
        toolbarsubTitle.isVisible = !title.isNullOrBlank()
        toolbarsubTitle.text = title
        super.setSubtitle(null)
    }

    override fun setCustomTitle(title: CharSequence?) {
        super.setCustomTitle(title)
        toolbarTitle.updateLayoutParams<LinearLayout.LayoutParams> {
            gravity = Gravity.START
        }
    }
}
