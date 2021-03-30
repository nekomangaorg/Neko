package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.CommonViewEmptyBinding
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.setVectorCompat
import eu.kanade.tachiyomi.util.view.visible

class EmptyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    RelativeLayout(context, attrs) {

    private val binding: CommonViewEmptyBinding =
        CommonViewEmptyBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Hide the information view
     */
    fun hide() {
        this.gone()
    }

    /**
     * Show the information view
     * @param textResource text of information view
     */
    fun show(@DrawableRes drawable: Int, @StringRes textResource: Int, actions: List<Action>? = null) {
        show(drawable, context.getString(textResource), actions)
    }

    /**
     * Show the information view
     * @param drawable icon of information view
     * @param textResource text of information view
     */
    fun show(@DrawableRes drawable: Int, message: String, actions: List<Action>? = null) {
        binding.imageView.setVectorCompat(drawable, context.getResourceColor(android.R.attr.textColorHint))
        binding.textLabel.text = message

        binding.actionsContainer.removeAllViews()
        if (!actions.isNullOrEmpty()) {
            actions.forEach {
                val button = (
                    inflate(
                        context,
                        R.layout.material_text_button,
                        null
                    ) as MaterialButton
                    ).apply {
                    setText(it.resId)
                    setOnClickListener(it.listener)
                }

                binding.actionsContainer.addView(button)
            }
        }

        this.visible()
    }

    data class Action(
        @StringRes val resId: Int,
        val listener: OnClickListener
    )
}
