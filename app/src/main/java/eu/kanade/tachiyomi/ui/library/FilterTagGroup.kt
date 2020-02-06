package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.filter_buttons.view.*

class FilterTagGroup@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null): LinearLayout
    (context, attrs) {

    var onItemClicked: (View, Int) -> Unit = { _, _ ->  }

    private var itemCount = 0
    private var root:ViewGroup? = null

    fun setup(root: ViewGroup, firstText: Int, secondText: Int? = null, thirdText: Int? = null) {
        val text1 = context.getString(firstText)
        val text2 = if (secondText != null) context.getString(secondText) else null
        val text3 = if (thirdText != null) context.getString(thirdText) else null
        setup(root, text1, text2, text3)
    }

    fun setup(root: ViewGroup, firstText: String, secondText: String? = null, thirdText: String? =
        null) {
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
        if (index > 1)
            toggleButton(index)
    }

    private fun toggleButton(index: Int) {
        if (itemCount == 0) return
        if (itemCount == 1) {
            firstButton.isActivated = !firstButton.isActivated
            onItemClicked(this, if (firstButton.isActivated) index else -1)
            return
        }
        val buttons = mutableListOf(firstButton, secondButton)
        if (itemCount >= 3)
            buttons.add(thirdButton)
        val mainButton = buttons[index]
        buttons.remove(mainButton)
        val transition = AutoTransition()
        transition.duration = 150
        TransitionManager.beginDelayedTransition(root, transition)
        if (mainButton.isActivated) {
            mainButton.isActivated = false
            separator1.visible()
            onItemClicked(this, -1)
            if (itemCount >= 3)
                separator2.visible()
            buttons.forEach{ it.visible() }
        }
        else {
            mainButton.isActivated = true
            onItemClicked(this, index)
            buttons.forEach{ it.gone() }
            separator1.gone()
            if (itemCount >= 3) {
                separator2.gone()
            }
        }
    }
}