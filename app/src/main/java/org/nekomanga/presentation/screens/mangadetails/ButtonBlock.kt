package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.isMerged
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.CoverRippleTheme
import org.nekomanga.presentation.components.NekoColors

@Composable
fun ButtonBlock(
    manga: Manga,
    trackServiceCount: Int,
    themeBasedOffCover: Boolean = true,
    favoriteClick: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
) {

    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val isDarkTheme = isSystemInDarkTheme()
    val buttonColor = remember {
        when (themeBasedOffCover && manga.vibrantCoverColor != null) {
            true -> getButtonThemeColor(Color(manga.vibrantCoverColor!!), isDarkTheme)
            false -> secondaryColor
        }
    }

    val checkedButtonBackgroundColor = remember { Color(getCheckedBackgroundColor(buttonColor, surfaceColor)) }
    val checkedButtonColors = ButtonDefaults.outlinedButtonColors(containerColor = checkedButtonBackgroundColor)
    val checkedBorderStroke = BorderStroke(1.dp, Color.Transparent)

    val uncheckedButtonColors = ButtonDefaults.outlinedButtonColors()
    val uncheckedBorderStroke = BorderStroke(1.dp, NekoColors.outline.copy(alpha = .3f))
    val gapBetweenButtons = 8.dp
    val padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    val iconicsPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
    ) {

        CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {

            val favConfig = when (manga.favorite) {
                true -> ButtonConfig(icon = Icons.Filled.Favorite, buttonColors = checkedButtonColors, borderStroke = checkedBorderStroke, text = stringResource(R.string.in_library))
                false -> ButtonConfig(icon = Icons.Filled.FavoriteBorder, buttonColors = uncheckedButtonColors, borderStroke = uncheckedBorderStroke, text = stringResource(R.string.add_to_library))
            }

            OutlinedButton(
                colors = favConfig.buttonColors,
                onClick = favoriteClick, border = favConfig.borderStroke, contentPadding = padding,
            ) {
                ButtonContent(favConfig.icon!!, color = buttonColor, text = favConfig.text)
            }

            Gap(gapBetweenButtons)

            if (trackServiceCount >= 0) {
                val trackerConfig = when {
                    trackServiceCount > 0 -> ButtonConfig(
                        icon = Icons.Filled.Check,
                        buttonColors = checkedButtonColors,
                        borderStroke = checkedBorderStroke,
                        text = stringResource(R.string._tracked, trackServiceCount),
                    )
                    else -> ButtonConfig(icon = Icons.Filled.Sync, buttonColors = uncheckedButtonColors, borderStroke = uncheckedBorderStroke, text = stringResource(R.string.tracking))
                }


                OutlinedButton(onClick = trackingClick, colors = trackerConfig.buttonColors, border = trackerConfig.borderStroke, contentPadding = padding) {
                    ButtonContent(trackerConfig.icon!!, color = buttonColor, text = trackerConfig.text)
                }
            }

            Gap(gapBetweenButtons)


            OutlinedButton(onClick = artworkClick, border = uncheckedBorderStroke, contentPadding = iconicsPadding) {
                IconicsButtonContent(iIcon = MaterialDesignDx.Icon.gmf_art_track, color = buttonColor, text = stringResource(id = R.string.artwork), iconicsSize = 32.dp)
            }

            Gap(gapBetweenButtons)

            OutlinedButton(onClick = similarClick, border = uncheckedBorderStroke, contentPadding = padding) {
                ButtonContent(Icons.Filled.AccountTree, color = buttonColor, text = stringResource(R.string.similar_work))
            }

            Gap(gapBetweenButtons)

            val mergeConfig = when (manga.isMerged()) {
                true -> ButtonConfig(
                    iIcon = CommunityMaterial.Icon.cmd_check_decagram,
                    buttonColors = checkedButtonColors,
                    borderStroke = checkedBorderStroke,
                    text = stringResource(R.string.is_merged),
                )
                false -> ButtonConfig(
                    iIcon = CommunityMaterial.Icon3.cmd_source_merge,
                    buttonColors = uncheckedButtonColors,
                    borderStroke = uncheckedBorderStroke,
                    text = stringResource(R.string.is_not_merged),
                )
            }

            OutlinedButton(onClick = mergeClick, colors = mergeConfig.buttonColors, border = mergeConfig.borderStroke, contentPadding = iconicsPadding) {
                IconicsButtonContent(iIcon = mergeConfig.iIcon!!, color = buttonColor, text = mergeConfig.text, iconicsSize = 28.dp)
            }
        }


        Gap(gapBetweenButtons)

        OutlinedButton(onClick = linksClick, border = uncheckedBorderStroke, contentPadding = padding) {
            ButtonContent(icon = Icons.Filled.OpenInBrowser, color = buttonColor, text = stringResource(R.string.links))
        }

        Gap(gapBetweenButtons)

        OutlinedButton(onClick = shareClick, border = uncheckedBorderStroke, contentPadding = padding) {
            ButtonContent(icon = Icons.Filled.Share, color = buttonColor, text = stringResource(R.string.share))
        }
    }
}

@Composable
private fun RowScope.IconicsButtonContent(
    iIcon: IIcon,
    color: Color = MaterialTheme.colorScheme.primary,
    text: String,
    iconicsSize: Dp = 24.dp,
) {
    Image(asset = iIcon, contentDescription = null, modifier = Modifier.size(iconicsSize), colorFilter = ColorFilter.tint(color = color))
    ButtonText(text = text, color = color)
}

@Composable
private fun RowScope.ButtonContent(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
    ButtonText(text = text, color = color)
}

@Composable
private fun RowScope.ButtonText(text: String, color: Color) {
    if (text.isNotEmpty()) {
        Gap(8.dp)
        Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(color = color.copy(alpha = .8f), letterSpacing = (-.5).sp, fontWeight = FontWeight.Medium))
    }
}

/*private fun getThemedColor(buttonColor: Color, surfaceColor: Color, isNightMode: Boolean): Color {
    val dominant = ColorUtils.blendARGB(buttonColor.toArgb(), surfaceColor.toArgb(), 0.5f)
    val domLum = ColorUtils.calculateLuminance(dominant)
    val lumWrongForTheme =
        (if (isNightMode) domLum > 0.8 else domLum <= 0.2)
    return Color(
        ColorUtils.blendARGB(
            dominant,
            surfaceColor.toArgb(),
            if (lumWrongForTheme) 0.9f else 0.7f,
        ),
    )
}*/

private fun getButtonThemeColor(buttonColor: Color, isNightMode: Boolean): Color {

    val color1 = buttonColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(color1).toFloat()

    val color2 = when (isNightMode) {
        true -> Color.White.toArgb()
        false -> Color.Black.toArgb()
    }

    val ratio = when (isNightMode) {
        true -> (-(luminance - 1)) * .33f
        false -> luminance * .5f
    }

    return when ((isNightMode && luminance <= 0.6) || (isNightMode.not() && luminance > 0.4)) {
        true -> Color(ColorUtils.blendARGB(color1, color2, ratio))
        false -> buttonColor
    }
}

private fun getCheckedBackgroundColor(buttonColor: Color, surfaceColor: Color): Int {
    return ColorUtils.blendARGB(buttonColor.toArgb(), surfaceColor.toArgb(), .706f)
}

private data class ButtonConfig(val icon: ImageVector? = null, val iIcon: IIcon? = null, val buttonColors: ButtonColors, val borderStroke: BorderStroke, val text: String)

