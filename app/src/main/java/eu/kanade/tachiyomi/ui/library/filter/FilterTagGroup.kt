package eu.kanade.tachiyomi.ui.library.filter

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.filter_buttons.view.*

class FilterTagGroup@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null): LinearLayout
    (context, attrs) {

    private var listener: FilterTagGroupListener? = null

    var itemCount = 0
        private set

    private var root:ViewGroup? = null

    private val buttons by lazy { arrayOf(firstButton, secondButton, thirdButton) }
    private val separators by lazy { arrayOf(separator1, separator2) }

    override fun isActivated(): Boolean {
        return buttons.any { it.isActivated }
    }

    fun nameOf(index: Int):String? = buttons.getOrNull(index)?.text as? String

    fun setup(root: ViewGroup, firstText: Int, secondText: Int? = null, thirdText: Int? = null) {
        val text1 = context.getString(firstText)
        val text2 = if (secondText != null) context.getString(secondText) else null
        val text3 = if (thirdText != null) context.getString(thirdText) else null
        setup(root, text1, text2, text3)
    }

    fun setup(root: ViewGroup, firstText: String, secondText: String? = null, thirdText: String? =
        null) {
        listener = root as? FilterTagGroupListener
        (layoutParams as? MarginLayoutParams)?.rightMargin = 5.dpToPx
        (layoutParams as? MarginLayoutParams)?.leftMargin = 5.dpToPx
        firstButton.text = firstText
        if (secondText != null) {
            secondButton.text = secondText
            itemCount = 2
            if (thirdText != null) {
                thirdButton.text = thirdText
                itemCount = 3
            }
            else {
                thirdButton.gone()
                separator2.gone()
            }
        }
        else {
            itemCount = 1
            secondButton.gone()
            separator1.gone()
            thirdButton.gone()
            separator2.gone()
        }
        this.root = root
        firstButton.setOnClickListener {toggleButton(0) }
        secondButton.setOnClickListener {toggleButton(1) }
        thirdButton.setOnClickListener {toggleButton(2) }
    }

    fun setState(preference: Preference<Int>) {
        val index = preference.getOrDefault() - 1
        if (index > -1)
            toggleButton(index, false)
    }

    fun setState(enabled: Boolean) {
        if (enabled)
            toggleButton(0, false)
    }

    fun reset() {
        buttons.forEach {
            it.isActivated = false
        }
        for (i in 0 until itemCount)  {
            buttons[i].visible()
            buttons[i].setTextColor(context.getResourceColor(android.R.attr.textColorPrimary))
        }
        for (i in 0 until (itemCount - 1)) separators[i].visible()
    }

    private fun toggleButton(index: Int, callBack: Boolean = true) {
        if (itemCount == 0) return
        if (callBack) {
            val transition = androidx.transition.AutoTransition()
            transition.duration = 150
            androidx.transition.TransitionManager.beginDelayedTransition(
                parent.parent as ViewGroup, transition
            )
        }
        if (itemCount == 1) {
            firstButton.isActivated = !firstButton.isActivated
            firstButton.setTextColor(if (firstButton.isActivated) Color.WHITE else context
                .getResourceColor(android.R.attr.textColorPrimary))
            listener?.onFilterClicked(this, if (firstButton.isActivated) index else -1, callBack)
            return
        }
        val buttons = mutableListOf(firstButton, secondButton)
        if (itemCount >= 3)
            buttons.add(thirdButton)
        val mainButton = buttons[index]
        buttons.remove(mainButton)

        if (mainButton.isActivated) {
            mainButton.isActivated = false
            separator1.visible()
            listener?.onFilterClicked(this, -1, callBack)
            if (itemCount >= 3)
                separator2.visible()
            buttons.forEach{ it.visible() }
        }
        else {
            mainButton.isActivated = true
            listener?.onFilterClicked(this, index, callBack)
            buttons.forEach{ it.gone() }
            separator1.gone()
            if (itemCount >= 3) {
                separator2.gone()
            }
        }
        mainButton.setTextColor(if (mainButton.isActivated) Color.WHITE else context
            .getResourceColor(android.R.attr.textColorPrimary))
    }
}

interface FilterTagGroupListener {
    fun onFilterClicked(view: FilterTagGroup, index: Int, updatePreference:Boolean)
}