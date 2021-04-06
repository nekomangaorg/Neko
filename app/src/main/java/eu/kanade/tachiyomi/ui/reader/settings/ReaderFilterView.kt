package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Window
import android.view.WindowManager
import android.widget.SeekBar
import androidx.annotation.ColorInt
import eu.kanade.tachiyomi.databinding.ReaderColorFilterBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlin.math.max

class ReaderFilterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderColorFilterBinding>(context, attrs) {

    var window: Window? = null
    private val boundingBox: Rect = Rect()
    private val exclusions = listOf(boundingBox)

    override fun inflateBinding() = ReaderColorFilterBinding.bind(this)
    override fun initGeneralPreferences() {
        activity = context as? ReaderActivity ?: return
        preferences.colorFilter().asFlow()
            .onEach { setColorFilter(it) }
            .launchIn(activity.scope)

        preferences.colorFilterMode().asFlow()
            .onEach { setColorFilter(preferences.colorFilter().get()) }
            .launchIn(activity.scope)

        preferences.customBrightness().asFlow()
            .onEach { setCustomBrightness(it) }
            .launchIn(activity.scope)

        // Get color and update values
        val color = preferences.colorFilterValue().get()
        val brightness = preferences.customBrightnessValue().get()

        val argb = setValues(color)

        // Set brightness value
        binding.txtBrightnessSeekbarValue.text = brightness.toString()
        binding.brightnessSeekbar.progress = brightness

        // Initialize seekBar progress
        binding.seekbarColorFilterAlpha.progress = argb[0]
        binding.seekbarColorFilterRed.progress = argb[1]
        binding.seekbarColorFilterGreen.progress = argb[2]
        binding.seekbarColorFilterBlue.progress = argb[3]

        // Set listeners
        binding.switchColorFilter.isChecked = preferences.colorFilter().get()
        binding.switchColorFilter.setOnCheckedChangeListener { _, isChecked ->
            preferences.colorFilter().set(isChecked)
        }

        binding.customBrightness.isChecked = preferences.customBrightness().get()
        binding.customBrightness.setOnCheckedChangeListener { _, isChecked ->
            preferences.customBrightness().set(isChecked)
        }

        binding.colorFilterMode.bindToPreference(preferences.colorFilterMode())
        binding.seekbarColorFilterAlpha.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    binding.seekbarColorFilterRed.isEnabled = value > 0 && binding.seekbarColorFilterAlpha.isEnabled
                    binding.seekbarColorFilterGreen.isEnabled = value > 0 && binding.seekbarColorFilterAlpha.isEnabled
                    binding.seekbarColorFilterBlue.isEnabled = value > 0 && binding.seekbarColorFilterAlpha.isEnabled
                    if (fromUser) {
                        setColorValue(value, ALPHA_MASK, 24)
                    }
                }
            }
        )

        setColorFilterSeekBar(binding.switchColorFilter.isChecked)

        binding.seekbarColorFilterRed.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, RED_MASK, 16)
                    }
                }
            }
        )

        binding.seekbarColorFilterGreen.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, GREEN_MASK, 8)
                    }
                }
            }
        )

        binding.seekbarColorFilterBlue.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, BLUE_MASK, 0)
                    }
                }
            }
        )

        binding.brightnessSeekbar.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        preferences.customBrightnessValue().set(value)
                    }
                }
            }
        )
    }

    /**
     * Set enabled status of seekBars belonging to color filter
     * @param enabled determines if seekBar gets enabled
     */
    private fun setColorFilterSeekBar(enabled: Boolean) {
        binding.seekbarColorFilterRed.isEnabled = binding.seekbarColorFilterAlpha.progress > 0 && enabled
        binding.seekbarColorFilterGreen.isEnabled = binding.seekbarColorFilterAlpha.progress > 0 && enabled
        binding.seekbarColorFilterBlue.isEnabled = binding.seekbarColorFilterAlpha.progress > 0 && enabled
        binding.seekbarColorFilterAlpha.isEnabled = enabled
    }

    /**
     * Set enabled status of seekBars belonging to custom brightness
     * @param enabled value which determines if seekBar gets enabled
     */
    private fun setCustomBrightnessSeekBar(enabled: Boolean) {
        binding.brightnessSeekbar.isEnabled = enabled
    }

    /**
     * Set the text value's of color filter
     * @param color integer containing color information
     */
    private fun setValues(color: Int): Array<Int> {
        val alpha = getAlphaFromColor(color)
        val red = getRedFromColor(color)
        val green = getGreenFromColor(color)
        val blue = getBlueFromColor(color)

        // Initialize values
        binding.txtColorFilterAlphaValue.text = alpha.toString()
        binding.txtColorFilterRedValue.text = red.toString()
        binding.txtColorFilterGreenValue.text = green.toString()
        binding.txtColorFilterBlueValue.text = blue.toString()

        return arrayOf(alpha, red, green, blue)
    }

    /**
     * Manages the custom brightness value subscription
     * @param enabled determines if the subscription get (un)subscribed
     */
    private fun setCustomBrightness(enabled: Boolean) {
        if (enabled) {
            preferences.customBrightnessValue().asFlow()
                .sample(100)
                .onEach { setCustomBrightnessValue(it) }
                .launchIn(activity.scope)
        } else {
            setCustomBrightnessValue(0, true)
        }
        setCustomBrightnessSeekBar(enabled)
    }

    fun setWindowBrightness() {
        setCustomBrightnessValue(preferences.customBrightnessValue().get(), !preferences.customBrightness().get())
    }

    /**
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    private fun setCustomBrightnessValue(value: Int, isDisabled: Boolean = false) {
        // Set black overlay visibility.
        if (!isDisabled) {
            binding.txtBrightnessSeekbarValue.text = value.toString()
            window?.attributes = window?.attributes?.apply { screenBrightness = max(0.01f, value / 100f) }
        } else {
            window?.attributes = window?.attributes?.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        }
    }

    /**
     * Manages the color filter value subscription
     * @param enabled determines if the subscription get (un)subscribed
     * @param view view of the dialog
     */
    private fun setColorFilter(enabled: Boolean) {
        if (enabled) {
            preferences.colorFilterValue().asFlow()
                .sample(100)
                .onEach { setColorFilterValue(it) }
                .launchIn(activity.scope)
        }
        setColorFilterSeekBar(enabled)
    }

    /**
     * Sets the color filter overlay of the screen. Determined by HEX of integer
     * @param color hex of color.
     */
    private fun setColorFilterValue(@ColorInt color: Int) {
        setValues(color)
    }

    /**
     * Updates the color value in preference
     * @param color value of color range [0,255]
     * @param mask contains hex mask of chosen color
     * @param bitShift amounts of bits that gets shifted to receive value
     */
    fun setColorValue(color: Int, mask: Long, bitShift: Int) {
        val currentColor = preferences.colorFilterValue().get()
        val updatedColor = (color shl bitShift) or (currentColor and mask.inv().toInt())
        preferences.colorFilterValue().set(updatedColor)
    }

    /**
     * Returns the alpha value from the Color Hex
     * @param color color hex as int
     * @return alpha of color
     */
    fun getAlphaFromColor(color: Int): Int {
        return color shr 24 and 0xFF
    }

    /**
     * Returns the red value from the Color Hex
     * @param color color hex as int
     * @return red of color
     */
    fun getRedFromColor(color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Returns the blue value from the Color Hex
     * @param color color hex as int
     * @return blue of color
     */
    fun getBlueFromColor(color: Int): Int {
        return color and 0xFF
    }

    private companion object {
        /** Integer mask of alpha value **/
        const val ALPHA_MASK: Long = 0xFF000000

        /** Integer mask of red value **/
        const val RED_MASK: Long = 0x00FF0000

        /** Integer mask of green value **/
        const val GREEN_MASK: Long = 0x0000FF00

        /** Integer mask of blue value **/
        const val BLUE_MASK: Long = 0x000000FF
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= 29 && changed) {
            with(binding.brightnessSeekbar) {
                boundingBox.set(this.left, this.top, this.right, this.bottom)
                this.systemGestureExclusionRects = exclusions
            }
        }
    }
}

/**
 * Returns the green value from the Color Hex
 * @param color color hex as int
 * @return green of color
 */
fun ReaderFilterView.getGreenFromColor(color: Int): Int {
    return color shr 8 and 0xFF
}
