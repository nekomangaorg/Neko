package eu.kanade.tachiyomi.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.TriStateCheckBoxBinding
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.setAnimVectorCompat
import eu.kanade.tachiyomi.util.view.setVectorCompat
import kotlin.math.roundToInt

class TriStateCheckBox constructor(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    var useIndeterminateForIgnore: Boolean = false
        set(value) {
            field = value
            if (state == State.IGNORE) {
                updateDrawable()
            }
        }

    var skipInversed: Boolean = false
        set(value) {
            field = value
            if (field && state == State.IGNORE) {
                state = State.UNCHECKED
            }
        }

    var text: CharSequence
        get() {
            return binding.textView.text
        }
        set(value) {
            binding.textView.text = value
        }

    var state: State = State.UNCHECKED
        set(value) {
            field = value
            updateDrawable()
        }

    var isUnchecked: Boolean
        get() = state == State.UNCHECKED
        set(value) {
            state = if (value) State.UNCHECKED else State.CHECKED
        }

    var isChecked: Boolean
        get() = state == State.UNCHECKED
        set(value) {
            state = if (value) State.CHECKED else State.UNCHECKED
        }

    private val binding = TriStateCheckBoxBinding.inflate(
        LayoutInflater.from(context),
        this,
        false,
    )

    private val disabledAlpha = run {
        val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.disabledAlpha))
        val attrValue = typedArray.getFloat(0, 0f)
        typedArray.recycle()
        attrValue
    }
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null
    private val uncheckedColor = ColorStateList.valueOf(context.getResourceColor(R.attr.colorControlNormal))
    private val checkedColor = ColorStateList.valueOf(context.getResourceColor(R.attr.colorSecondary))
    private val inverseColor = ColorStateList.valueOf(context.getResourceColor(R.attr.colorSecondaryVariant))
    private val indeterColor = ColorStateList.valueOf(context.getResourceColor(R.attr.colorPrimary))
    private val ignoreColor get() = if (useIndeterminateForIgnore) indeterColor else inverseColor

    private val disabledColor = ColorStateList.valueOf(
        ColorUtils.setAlphaComponent(context.getResourceColor(R.attr.colorControlNormal), (disabledAlpha * 255).roundToInt()),
    )

    init {
        addView(binding.root)
        val a = context.obtainStyledAttributes(attrs, R.styleable.TriStateCheckBox, 0, 0)

        val str = a.getString(R.styleable.TriStateCheckBox_android_text) ?: ""
        text = str

        val maxLines = a.getInt(R.styleable.TriStateCheckBox_android_maxLines, Int.MAX_VALUE)
        binding.textView.maxLines = maxLines

        val resourceId = a.getResourceId(R.styleable.TriStateCheckBox_android_textAppearance, 0)
        if (resourceId != 0) {
            binding.textView.setTextAppearance(resourceId)
        }

        val textColor = a.getColor(R.styleable.TriStateCheckBox_android_textColor, 0)
        if (textColor != 0) {
            binding.textView.setTextColor(textColor)
        }

        val drawablePadding = a.getDimensionPixelSize(R.styleable.TriStateCheckBox_android_drawablePadding, 0)
        if (drawablePadding != 0) {
            binding.textView.updateLayoutParams<MarginLayoutParams> {
                marginStart = drawablePadding
            }
        }

        setOnClickListener {
            goToNextStep()
            mOnCheckedChangeListener?.onCheckedChanged(this, state)
        }
        isClickable = a.getBoolean(R.styleable.TriStateCheckBox_android_clickable, true)
        isFocusable = a.getBoolean(R.styleable.TriStateCheckBox_android_focusable, true)

        a.recycle()
    }

    fun goToNextStep() {
        setState(
            when (state) {
                State.CHECKED -> if (skipInversed) State.UNCHECKED else State.IGNORE
                State.UNCHECKED -> State.CHECKED
                else -> State.UNCHECKED
            },
            true,
        )
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (enabled) {
            binding.textView.alpha = 1f
            updateDrawable()
        } else {
            binding.textView.alpha = disabledAlpha
            binding.triStateBox.imageTintList = disabledColor
            binding.triStateBox.backgroundTintList = disabledColor
        }
    }

    fun setState(state: State, animated: Boolean = false) {
        if (animated) {
            animateDrawableToState(state)
        } else {
            this.state = state
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mOnCheckedChangeListener = listener
    }

    private fun animateDrawableToState(state: State) {
        val oldState = this.state
        if (state == oldState) return
        this.state = state
        with(binding.triStateBox) {
            when (state) {
                State.UNCHECKED -> {
                    setAnimVectorCompat(
                        when (oldState) {
                            State.IGNORE -> if (useIndeterminateForIgnore) {
                                R.drawable.anim_checkbox_indeterminate_to_blank_24dp
                            } else {
                                R.drawable.anim_check_box_x_to_blank_24dp
                            }
                            else -> R.drawable.anim_check_box_checked_to_blank_24dp
                        },
                    )
                    backgroundTintList = uncheckedColor
                }
                State.CHECKED -> {
                    setAnimVectorCompat(R.drawable.anim_check_box_blank_to_checked_24dp)
                    backgroundTintList = checkedColor
                }
                State.IGNORE -> {
                    setAnimVectorCompat(
                        when {
                            useIndeterminateForIgnore -> R.drawable.anim_check_box_checked_to_indeterminate_24dp
                            oldState == State.CHECKED -> R.drawable.anim_check_box_checked_to_x_24dp
                            else -> R.drawable.anim_checkbox_blank_to_x_24dp
                        },
                    )
                    backgroundTintList = ignoreColor
                }
            }
            if (this@TriStateCheckBox.isEnabled) imageTintList = backgroundTintList
        }
    }

    fun setCheckboxBackground(drawable: Drawable?) {
        binding.triStateBox.background = drawable
    }

    private fun updateDrawable() {
        with(binding.triStateBox) {
            backgroundTintList = when (state) {
                State.UNCHECKED -> {
                    setVectorCompat(R.drawable.ic_check_box_outline_blank_24dp)
                    uncheckedColor
                }
                State.CHECKED -> {
                    setVectorCompat(R.drawable.ic_check_box_24dp)
                    checkedColor
                }
                State.IGNORE -> {
                    setVectorCompat(
                        if (useIndeterminateForIgnore) {
                            R.drawable.ic_check_box_indeterminate_24dp
                        } else {
                            R.drawable.ic_check_box_x_24dp
                        },
                    )
                    ignoreColor
                }
            }
            if (this@TriStateCheckBox.isEnabled) imageTintList = backgroundTintList
        }
    }

    enum class State {
        UNCHECKED,
        CHECKED,
        IGNORE,
    }

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    fun interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param state The new checked state of buttonView.
         */
        fun onCheckedChanged(buttonView: TriStateCheckBox, state: State)
    }
}
