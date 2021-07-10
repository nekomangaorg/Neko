package eu.kanade.tachiyomi.ui.reader.settings

import android.content.pm.ActivityInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class OrientationType(val prefValue: Int, val flag: Int, @StringRes val stringRes: Int, @DrawableRes val iconRes: Int, val flagValue: Int) {
    DEFAULT(0, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.default_value, R.drawable.ic_screen_rotation_24dp, 0x00000000),
    FREE(1, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.free, R.drawable.ic_screen_rotation_24dp, 0x00000008),
    PORTRAIT(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, R.string.portrait, R.drawable.ic_stay_current_portrait_24dp, 0x00000010),
    LANDSCAPE(3, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, R.string.landscape, R.drawable.ic_stay_current_landscape_24dp, 0x00000018),
    LOCKED_PORTRAIT(4, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, R.string.locked_portrait, R.drawable.ic_screen_lock_portrait_24dp, 0x00000020),
    LOCKED_LANDSCAPE(5, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, R.string.locked_landscape, R.drawable.ic_screen_lock_landscape_24dp, 0x00000028),
    ;

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int): OrientationType =
            values().find { it.flagValue == preference } ?: FREE

        fun fromSpinner(position: Int?) = values().find { value -> value.prefValue == position } ?: DEFAULT
    }
}
