package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import eu.kanade.tachiyomi.databinding.ReaderColorFilterBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import kotlin.math.max
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample

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

        binding.grayscale.bindToPreference(preferences.grayscale())
        binding.invertedColors.bindToPreference(preferences.invertedColors())

        // Get color and update values
        val color = preferences.colorFilterValue().get()
        val brightness = preferences.customBrightnessValue().get()

        val argb = setValues(color)

        // Set brightness value
        binding.txtBrightnessSliderValue.text = brightness.toString()
        binding.brightnessSlider.value = brightness.toFloat()

        // Initialize slider values
        binding.sliderColorFilterAlpha.value = argb[0].toFloat()
        binding.sliderColorFilterRed.value = argb[1].toFloat()
        binding.sliderColorFilterGreen.value = argb[2].toFloat()
        binding.sliderColorFilterBlue.value = argb[3].toFloat()

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
        binding.sliderColorFilterAlpha.addOnChangeListener { _, value, fromUser ->
            binding.sliderColorFilterRed.isEnabled =
                value > 0 && binding.sliderColorFilterAlpha.isEnabled
            binding.sliderColorFilterGreen.isEnabled =
                value > 0 && binding.sliderColorFilterAlpha.isEnabled
            binding.sliderColorFilterBlue.isEnabled =
                value > 0 && binding.sliderColorFilterAlpha.isEnabled
            if (fromUser) {
                setColorValue(value.toInt(), ALPHA_MASK, 24)
            }
        }

        setColorFilterSlider(binding.switchColorFilter.isChecked)

        binding.sliderColorFilterRed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                setColorValue(value.toInt(), RED_MASK, 16)
            }
        }

        binding.sliderColorFilterGreen.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                setColorValue(value.toInt(), GREEN_MASK, 8)
            }
        }

        binding.sliderColorFilterBlue.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                setColorValue(value.toInt(), BLUE_MASK, 0)
            }
        }

        binding.brightnessSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                preferences.customBrightnessValue().set(value.toInt())
            }
        }
    }

    /**
     * Set enabled status of sliders belonging to color filter
     * @param enabled determines if the sliders get enabled
     */
    private fun setColorFilterSlider(enabled: Boolean) {
        binding.sliderColorFilterRed.isEnabled = binding.sliderColorFilterAlpha.value > 0 && enabled
        binding.sliderColorFilterGreen.isEnabled = binding.sliderColorFilterAlpha.value > 0 && enabled
        binding.sliderColorFilterBlue.isEnabled = binding.sliderColorFilterAlpha.value > 0 && enabled
        binding.sliderColorFilterAlpha.isEnabled = enabled
    }

    /**
     * Set enabled status of sliders belonging to custom brightness
     * @param enabled value which determines if slider gets enabled
     */
    private fun setCustomBrightnessSlider(enabled: Boolean) {
        binding.brightnessSlider.isEnabled = enabled
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
        setCustomBrightnessSlider(enabled)
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
            binding.txtBrightnessSliderValue.text = value.toString()
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
        setColorFilterSlider(enabled)
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
            with(binding.brightnessSlider) {
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
