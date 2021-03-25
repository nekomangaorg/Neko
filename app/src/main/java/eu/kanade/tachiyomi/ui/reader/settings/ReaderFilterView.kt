package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.annotation.ColorInt
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.android.synthetic.main.reader_color_filter.*
import kotlinx.android.synthetic.main.reader_color_filter.view.*
import kotlinx.android.synthetic.main.reader_general_layout.view.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample

class ReaderFilterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView(context, attrs) {

    override fun initGeneralPreferences() {
        activity = context as ReaderActivity
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
        txt_brightness_seekbar_value.text = brightness.toString()
        brightness_seekbar.progress = brightness

        // Initialize seekBar progress
        seekbar_color_filter_alpha.progress = argb[0]
        seekbar_color_filter_red.progress = argb[1]
        seekbar_color_filter_green.progress = argb[2]
        seekbar_color_filter_blue.progress = argb[3]

        // Set listeners
        switch_color_filter.isChecked = preferences.colorFilter().get()
        switch_color_filter.setOnCheckedChangeListener { _, isChecked ->
            preferences.colorFilter().set(isChecked)
        }

        custom_brightness.isChecked = preferences.customBrightness().get()
        custom_brightness.setOnCheckedChangeListener { _, isChecked ->
            preferences.customBrightness().set(isChecked)
        }

        color_filter_mode.bindToPreference(preferences.colorFilterMode())
        seekbar_color_filter_alpha.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    seekbar_color_filter_red.isEnabled = value > 0 && seekbar_color_filter_alpha.isEnabled
                    seekbar_color_filter_green.isEnabled = value > 0 && seekbar_color_filter_alpha.isEnabled
                    seekbar_color_filter_blue.isEnabled = value > 0 && seekbar_color_filter_alpha.isEnabled
                    if (fromUser) {
                        setColorValue(value, ALPHA_MASK, 24)
                    }
                }
            }
        )

        seekbar_color_filter_red.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, RED_MASK, 16)
                    }
                }
            }
        )

        seekbar_color_filter_green.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, GREEN_MASK, 8)
                    }
                }
            }
        )

        seekbar_color_filter_blue.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, BLUE_MASK, 0)
                    }
                }
            }
        )

        brightness_seekbar.setOnSeekBarChangeListener(
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
        seekbar_color_filter_red.isEnabled = seekbar_color_filter_alpha.progress > 0 && enabled
        seekbar_color_filter_green.isEnabled = seekbar_color_filter_alpha.progress > 0 && enabled
        seekbar_color_filter_blue.isEnabled = seekbar_color_filter_alpha.progress > 0 && enabled
        seekbar_color_filter_alpha.isEnabled = enabled
    }

    /**
     * Set enabled status of seekBars belonging to custom brightness
     * @param enabled value which determines if seekBar gets enabled
     */
    private fun setCustomBrightnessSeekBar(enabled: Boolean) {
        brightness_seekbar.isEnabled = enabled
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
        txt_color_filter_alpha_value.text = alpha.toString()
        txt_color_filter_red_value.text = red.toString()
        txt_color_filter_green_value.text = green.toString()
        txt_color_filter_blue_value.text = blue.toString()

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

    /**
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    private fun setCustomBrightnessValue(value: Int, isDisabled: Boolean = false) {
        // Set black overlay visibility.
        if (!isDisabled) {
            txt_brightness_seekbar_value.text = value.toString()
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
}

/**
 * Returns the green value from the Color Hex
 * @param color color hex as int
 * @return green of color
 */
fun ReaderFilterView.getGreenFromColor(color: Int): Int {
    return color shr 8 and 0xFF
}
