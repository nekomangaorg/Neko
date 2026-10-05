package eu.kanade.tachiyomi.ui.reader.settings

import android.graphics.Color as AndroidColor
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import org.nekomanga.R

enum class ReaderTheme(
    val prefValue: Int,
    @param:StringRes val stringRes: Int,
    @param:StringRes val settingsStringRes: Int = stringRes,
) {
    WHITE(0, R.string.white),
    BLACK(1, R.string.black),
    SMART_BY_PAGE(2, R.string.smart_by_page, R.string.smart_based_on_page),
    SMART_BY_THEME(3, R.string.smart_by_theme, R.string.smart_based_on_page_and_theme),
    SMART_BY_THEME_BUT_BLACK(
        4,
        R.string.smart_by_theme_but_black,
        R.string.smart_based_on_page_and_theme_use_black,
    );

    val isSmart: Boolean
        get() = this == SMART_BY_PAGE || this == SMART_BY_THEME || this == SMART_BY_THEME_BUT_BLACK

    fun color(themeBackground: ComposeColor): ComposeColor {
        return when (this) {
            WHITE -> ComposeColor.White
            BLACK,
            SMART_BY_THEME_BUT_BLACK -> ComposeColor.Black
            SMART_BY_PAGE,
            SMART_BY_THEME -> themeBackground
        }
    }

    @Composable
    @ReadOnlyComposable
    fun color(): ComposeColor = color(MaterialTheme.colorScheme.background)

    /**
     * Color behind a page whose edges are not dark, or null when this theme does not look at the
     * page.
     */
    fun smartBaseColor(themeBackground: ComposeColor): ComposeColor? {
        return when (this) {
            WHITE,
            BLACK -> null
            SMART_BY_PAGE -> ComposeColor.White
            SMART_BY_THEME -> themeBackground
            SMART_BY_THEME_BUT_BLACK ->
                if (themeBackground.isWhite()) {
                    themeBackground
                } else {
                    ComposeColor.Black
                }
        }
    }

    fun androidColor(isDark: Boolean): Int {
        return when (this) {
            WHITE -> AndroidColor.WHITE
            BLACK,
            SMART_BY_THEME_BUT_BLACK -> AndroidColor.BLACK
            SMART_BY_PAGE,
            SMART_BY_THEME -> if (isDark) AndroidColor.BLACK else AndroidColor.WHITE
        }
    }

    // The white threshold SmartBackgroundAnalyzer uses for page edges.
    private fun ComposeColor.isWhite(): Boolean {
        val argb = toArgb()
        return ((argb shr 16) and 0xFF) + ((argb shr 8) and 0xFF) + (argb and 0xFF) > 740
    }

    companion object {
        fun fromPreference(preference: Int?): ReaderTheme =
            entries.find { it.prefValue == preference } ?: SMART_BY_PAGE
    }
}
