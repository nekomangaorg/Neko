package eu.kanade.tachiyomi.util.system

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import kotlin.math.roundToInt

@Suppress("unused")
enum class Themes(
    @StyleRes val styleRes: Int,
    val nightMode: Int,
    @StringRes val nameRes: Int,
    @StringRes altNameRes: Int? = null,
) {
    DEFAULT(
        R.style.Theme_Tachiyomi,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.white_theme,
        R.string.dark
    ),
    SPRING_AND_DUSK(
        R.style.Theme_Tachiyomi_MidnightDusk,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.spring_blossom,
        R.string.midnight_dusk
    ),
    STRAWBERRIES(
        R.style.Theme_Tachiyomi_Strawberries,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.strawberry_daiquiri,
        R.string.chocolate_strawberries
    ),
    OUTRUN(
        R.style.Theme_Tachiyomi_Outrun,
        AppCompatDelegate.MODE_NIGHT_YES,
        R.string.outrun
    ),

    LIME(
        R.style.Theme_Tachiyomi_FlatLime,
        AppCompatDelegate.MODE_NIGHT_YES,
        R.string.flat_lime
    ),
    MANGADEX(
        R.style.Theme_Tachiyomi_MangaDex,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.mangadex
    );

    val isDarkTheme = nightMode == AppCompatDelegate.MODE_NIGHT_YES
    val followsSystem = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    @StringRes
    val darkNameRes: Int = altNameRes ?: nameRes

    fun getColors(mode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM): Colors {
        return when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> darkColors()
            AppCompatDelegate.MODE_NIGHT_NO -> lightColors()
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> when (mode) {
                AppCompatDelegate.MODE_NIGHT_YES -> darkColors()
                else -> lightColors()
            }
            else -> lightColors()
        }
    }

    private fun lightColors(): Colors {
        return Colors(
            lightPrimaryText,
            lightSecondaryText,
            lightBackground,
            lightAccent,
            lightAppBar,
            lightAppBarText,
            lightBottomBar,
            lightInactiveTab,
            lightActiveTab,
        )
    }

    private fun darkColors(): Colors {
        return Colors(
            darkPrimaryText,
            darkSecondaryText,
            darkBackground,
            darkAccent,
            darkAppBar,
            darkAppBarText,
            darkBottomBar,
            darkInactiveTab,
            darkActiveTab,
        )
    }

    /** Complies with textColorPrimary (probably night) */
    @ColorInt
    val lightPrimaryText: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_MidnightDusk -> "#DE240728"
            else -> "#DE000000"
        }
    )

    /** Complies with textColorPrimary (probably night) */
    @ColorInt
    val darkPrimaryText: Int = Color.parseColor("#FFFFFFFF")

    /** Complies with textColorSecondary (primary with alpha) */
    @ColorInt
    val lightSecondaryText: Int =
        ColorUtils.setAlphaComponent(lightPrimaryText, (0.54f * 255f).roundToInt())

    /** Complies with textColorSecondary (primary with alpha) */
    @ColorInt
    val darkSecondaryText: Int =
        ColorUtils.setAlphaComponent(darkPrimaryText, (0.54f * 255f).roundToInt())

    /** Complies with colorBackground */
    @ColorInt
    val lightBackground: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_MidnightDusk -> "#f6f0f8"
            else -> "#FAFAFA"
        }
    )

    /** Complies with colorBackground (probably night) */
    @ColorInt
    val darkBackground: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_Strawberries -> "#1a1716"
            R.style.Theme_Tachiyomi_MidnightDusk -> "#16151D"
            R.style.Theme_Tachiyomi_FlatLime -> "#202125"
            R.style.Theme_Tachiyomi, R.style.Theme_Tachiyomi_MangaDex -> "#292929"
            R.style.Theme_Tachiyomi_Outrun -> "#170c32"
            else -> "#1C1C1D"
        }
    )

    /** Complies with colorAccent */
    @ColorInt
    val lightAccent: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_MidnightDusk -> "#c43c97"
            R.style.Theme_Tachiyomi_Strawberries -> "#ED4A65"
            R.style.Theme_Tachiyomi_MangaDex -> "#FF6740"
            R.style.Theme_Tachiyomi -> "#101820"
            else -> "#2979FF"
        }
    )

    /** Complies with colorAccent (probably night) */
    @ColorInt
    val darkAccent: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_MidnightDusk -> "#F02475"
            R.style.Theme_Tachiyomi_Strawberries -> "#AA2200"
            R.style.Theme_Tachiyomi_FlatLime -> "#4AF88A"
            R.style.Theme_Tachiyomi -> "#20aa5e"
            R.style.Theme_Tachiyomi_MangaDex -> "#FF6740"
            R.style.Theme_Tachiyomi_Outrun -> "#dd546e"
            else -> "#3399FF"
        }
    )

    /** Complies with colorSecondary */
    @ColorInt
    val lightAppBar: Int = when (styleRes) {
        else -> lightBackground
    }

    /** Complies with colorSecondary (probably night) */
    @ColorInt
    val darkAppBar: Int = when (styleRes) {
        else -> darkBackground
    }

    /** Complies with actionBarTintColor */
    @ColorInt
    val lightAppBarText: Int = when (styleRes) {
        R.style.Theme_Tachiyomi_MidnightDusk -> Color.parseColor("#DE4c0d4b")
        else -> lightPrimaryText
    }

    /** Complies with actionBarTintColor (probably night) */
    @ColorInt
    val darkAppBarText: Int = when (styleRes) {
        else -> darkPrimaryText
    }

    /** Complies with colorPrimaryVariant */
    @ColorInt
    val lightBottomBar: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_MidnightDusk -> "#efe3f3"
            else -> "#FFFFFF"
        }
    )

    /** Complies with colorPrimaryVariant (probably night) */
    @ColorInt
    val darkBottomBar: Int = Color.parseColor(
        when (styleRes) {
            R.style.Theme_Tachiyomi_Strawberries -> "#211b19"
            R.style.Theme_Tachiyomi, R.style.Theme_Tachiyomi_MangaDex -> "#292929"
            R.style.Theme_Tachiyomi_MidnightDusk -> "#201F27"
            R.style.Theme_Tachiyomi_FlatLime -> "#282A2E"
            R.style.Theme_Tachiyomi_Outrun -> "#201147"
            else -> "#212121"
        }
    )

    /** Complies with tabBarIconInactive */
    @ColorInt
    val lightInactiveTab: Int = when (styleRes) {
        else -> Color.parseColor("#C2424242")
    }

    /** Complies with tabBarIconInactive (probably night) */
    @ColorInt
    val darkInactiveTab: Int = when (styleRes) {
        else -> Color.parseColor("#C2FFFFFF")
    }

    /** Complies with tabBarIconColor or colorAccent */
    @ColorInt
    val lightActiveTab: Int = when (styleRes) {
        else -> lightAccent
    }

    /** Complies with tabBarIconColor or colorAccent (probably night) */
    @ColorInt
    val darkActiveTab: Int = when (styleRes) {
        else -> darkAccent
    }

    data class Colors(
        @ColorInt val primaryText: Int,
        @ColorInt val secondaryText: Int,
        @ColorInt val colorBackground: Int,
        @ColorInt val colorAccent: Int,
        /** Complies with colorSecondary */
        @ColorInt val appBar: Int,
        /** Complies with actionBarTintColor */
        @ColorInt val appBarText: Int,
        /** Complies with colorPrimaryVariant */
        @ColorInt val bottomBar: Int,
        /** Complies with tabBarIconInactive */
        @ColorInt val inactiveTab: Int,
        /** Complies with tabBarIconColor */
        @ColorInt val activeTab: Int,
    )
}
