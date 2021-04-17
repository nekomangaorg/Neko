package eu.kanade.tachiyomi.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.widget.TextViewCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MenuSheetItemBinding

class MenuSheetItemView @JvmOverloads constructor(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {
    private val mText: String
    private val mIconRes: Int
    private val mEndIconRes: Int

    private var binding: MenuSheetItemBinding? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MenuSheetItemView, 0, 0)

        val str = a.getString(R.styleable.MenuSheetItemView_android_text) ?: ""
        mText = str

        val d = a.getResourceId(R.styleable.MenuSheetItemView_icon, 0)
        mIconRes = d

        val e = a.getResourceId(R.styleable.MenuSheetItemView_endIcon, 0)
        mEndIconRes = e

        a.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = try {
            MenuSheetItemBinding.bind(this)
        } catch (e: Exception) {
            MenuSheetItemBinding.inflate(LayoutInflater.from(context), this, true)
        }
        text = mText
        setIcon(mIconRes)
        setEndIcon(mEndIconRes)
    }

    var text: CharSequence?
        get() = binding?.itemTextView?.text
        set(value) {
            binding?.itemTextView?.text = value
        }

    var textSize: Float
        get() = binding?.itemTextView?.textSize ?: 0f
        set(value) {
            binding?.itemTextView?.textSize = value
        }

    fun setText(@StringRes res: Int) {
        text = context.getString(res)
    }

    fun setTextColor(@ColorInt color: Int) {
        binding?.itemTextView?.setTextColor(color)
    }

    fun setIconColor(@ColorInt color: Int) = binding?.itemTextView?.let {
        TextViewCompat.setCompoundDrawableTintList(
            it,
            ColorStateList.valueOf(color)
        )
    }

    fun setIcon(@DrawableRes res: Int) {
        binding?.itemTextView?.setCompoundDrawablesRelativeWithIntrinsicBounds(
            res,
            0,
            0,
            0
        )
    }

    fun setEndIcon(@DrawableRes res: Int) {
        binding?.menuEndItem?.isInvisible = res == 0
        binding?.menuEndItem?.setImageResource(res)
    }

    fun setEndIcon(drawable: Drawable?) {
        binding?.menuEndItem?.isInvisible = drawable == null
        binding?.menuEndItem?.setImageDrawable(drawable)
    }
}
