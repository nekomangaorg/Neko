package eu.kanade.tachiyomi.util.system

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import eu.kanade.tachiyomi.R

@Suppress("unused")
enum class Themes(
    @StyleRes val styleRes: Int,
    val nightMode: Int,
    @StringRes val nameRes: Int,
    @StringRes altNameRes: Int? = null,
) {
    MONET(
        R.style.Theme_Tachiyomi_Monet,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.a_brighter_you,
        R.string.a_calmer_you,
    ),
    DEFAULT(
        R.style.Theme_Tachiyomi,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.white_theme,
        R.string.dark,
    ),
    OUTRUN(
        R.style.Theme_Tachiyomi_Outrun,
        AppCompatDelegate.MODE_NIGHT_YES,
        R.string.outrun,
    ),
    SPRING_AND_DUSK(
        R.style.Theme_Tachiyomi_MidnightDusk,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.spring_blossom,
        R.string.midnight_dusk,
    ),
    MANGADEX(
        R.style.Theme_Tachiyomi_MangaDex,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.mangadex,
    ),
    TEAL_AND_SAPPHIRE(
        R.style.Theme_Tachiyomi_SapphireDusk,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.teal_ocean,
        R.string.sapphire_dusk,
    ),
    LAVENDER(
        R.style.Theme_Tachiyomi_Lavender,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.lavender,
        R.string.violet,
    ),
    GREY_WOLF(
        R.style.Theme_Tachiyomi_GreyWolf,
        AppCompatDelegate.MODE_NIGHT_YES,
        R.string.grey_wolf,
    ),
    STRAWBERRIES(
        R.style.Theme_Tachiyomi_Strawberries,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.strawberry_daiquiri,
        R.string.chocolate_strawberries,
    ),
    TAKO(
        R.style.Theme_Tachiyomi_Tako,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.tako,
    ),

    LIME(
        R.style.Theme_Tachiyomi_FlatLime,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.lime_time,
        R.string.flat_lime,
    ),
    ;

    val isDarkTheme = nightMode == AppCompatDelegate.MODE_NIGHT_YES
    val followsSystem = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    @StringRes
    val darkNameRes: Int = altNameRes ?: nameRes
}
