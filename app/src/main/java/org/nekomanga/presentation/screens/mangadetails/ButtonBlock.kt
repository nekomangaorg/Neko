package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

/**
 * Block of buttons for the actions on the backdrop screen
 */
@Composable
fun ButtonBlock(
    hideButtonTextProvider: () -> Boolean,
    isInitializedProvider: () -> Boolean,
    isMergedProvider: () -> Boolean,
    inLibraryProvider: () -> Boolean,
    loggedIntoTrackersProvider: () -> Boolean,
    trackServiceCountProvider: () -> Int,
    themeColorState: ThemeColorState,
    favoriteClick: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
) {
    if (!isInitializedProvider()) {
        return
    }

    val shape = RoundedCornerShape(35)

    val checkedButtonColors = ButtonDefaults.outlinedButtonColors(containerColor = themeColorState.altContainerColor)
    val checkedBorderStroke = BorderStroke(1.dp, Color.Transparent)

    val uncheckedButtonColors = ButtonDefaults.outlinedButtonColors()
    val uncheckedBorderStroke = BorderStroke(1.dp, themeColorState.altContainerColor.copy(alpha = .8f))
    val gapBetweenButtons = 8.dp
    val (padding, iconicsPadding, buttonModifier) = when (hideButtonTextProvider()) {
        true -> Triple(PaddingValues(0.dp), PaddingValues(0.dp), Modifier.size(48.dp))
        false -> Triple(PaddingValues(horizontal = 12.dp, vertical = 8.dp), PaddingValues(horizontal = 12.dp, vertical = 4.dp), Modifier.height(48.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val favConfig = when (inLibraryProvider()) {
            true -> ButtonConfig(icon = Icons.Filled.Favorite, buttonColors = checkedButtonColors, borderStroke = checkedBorderStroke, text = stringResource(R.string.in_library))
            false -> ButtonConfig(icon = Icons.Filled.FavoriteBorder, buttonColors = uncheckedButtonColors, borderStroke = uncheckedBorderStroke, text = stringResource(R.string.add_to_library))
        }

        OutlinedButton(
            colors = favConfig.buttonColors,
            modifier = Modifier.size(48.dp),
            shape = shape,
            onClick = favoriteClick,
            border = favConfig.borderStroke,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(imageVector = favConfig.icon!!, contentDescription = null, modifier = Modifier.size(24.dp), tint = themeColorState.buttonColor)
        }

        if (loggedIntoTrackersProvider()) {
            Gap(gapBetweenButtons)

            val trackerConfig = when {
                trackServiceCountProvider() > 0 -> ButtonConfig(
                    icon = Icons.Filled.Check,
                    buttonColors = checkedButtonColors,
                    borderStroke = checkedBorderStroke,
                    text = stringResource(R.string._tracked, trackServiceCountProvider()),
                )
                else -> ButtonConfig(icon = Icons.Filled.Sync, buttonColors = uncheckedButtonColors, borderStroke = uncheckedBorderStroke, text = stringResource(R.string.tracking))
            }

            OutlinedButton(
                onClick = trackingClick,
                modifier = buttonModifier,
                shape = shape,
                colors = trackerConfig.buttonColors,
                border = trackerConfig.borderStroke,
                contentPadding = padding,
            ) {
                if (trackServiceCountProvider() > 0 && hideButtonTextProvider()) {
                    val icon = when (trackServiceCountProvider()) {
                        1 -> CommunityMaterial.Icon3.cmd_numeric_1_box_outline
                        2 -> CommunityMaterial.Icon3.cmd_numeric_2_box_outline
                        3 -> CommunityMaterial.Icon3.cmd_numeric_3_box_outline
                        4 -> CommunityMaterial.Icon3.cmd_numeric_4_box_outline
                        else -> CommunityMaterial.Icon3.cmd_traffic_cone
                    }
                    IconicsButtonContent(
                        iIcon = icon,
                        color = themeColorState.buttonColor,
                        hideText = hideButtonTextProvider(),
                        text = "",
                        iconicsSize = 28.dp,
                    )
                } else {
                    ButtonContent(trackerConfig.icon!!, color = themeColorState.buttonColor, hideText = hideButtonTextProvider(), text = trackerConfig.text)
                }
            }
        }

        Gap(gapBetweenButtons)

        OutlinedButton(
            onClick = artworkClick,
            modifier = buttonModifier,
            shape = shape,
            border = uncheckedBorderStroke,
            contentPadding = iconicsPadding,
        ) {
            IconicsButtonContent(
                iIcon = MaterialDesignDx.Icon.gmf_art_track,
                color = themeColorState.buttonColor,
                hideText = hideButtonTextProvider(),
                text = stringResource(id = R.string.artwork),
                iconicsSize = 32.dp,
            )
        }

        Gap(gapBetweenButtons)

        OutlinedButton(
            onClick = similarClick,
            modifier = buttonModifier,
            shape = shape,
            border = uncheckedBorderStroke,
            contentPadding = padding,
        ) {
            ButtonContent(Icons.Filled.AccountTree, color = themeColorState.buttonColor, hideText = hideButtonTextProvider(), text = stringResource(R.string.similar_work))
        }

        Gap(gapBetweenButtons)

        val mergeConfig = when (isMergedProvider()) {
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

        OutlinedButton(
            onClick = mergeClick,
            modifier = buttonModifier,
            shape = shape,
            colors = mergeConfig.buttonColors,
            border = mergeConfig.borderStroke,
            contentPadding = iconicsPadding,
        ) {
            IconicsButtonContent(iIcon = mergeConfig.iIcon!!, color = themeColorState.buttonColor, hideText = hideButtonTextProvider(), text = mergeConfig.text, iconicsSize = 28.dp)
        }

        Gap(gapBetweenButtons)

        OutlinedButton(
            onClick = linksClick,
            modifier = buttonModifier,
            shape = shape,
            border = uncheckedBorderStroke,
            contentPadding = padding,
        ) {
            ButtonContent(icon = Icons.Filled.OpenInBrowser, color = themeColorState.buttonColor, hideText = hideButtonTextProvider(), text = stringResource(R.string.links))
        }

        Gap(gapBetweenButtons)

        OutlinedButton(
            onClick = shareClick,
            modifier = buttonModifier,
            shape = shape,
            border = uncheckedBorderStroke,
            contentPadding = padding,
        ) {
            ButtonContent(icon = Icons.Filled.Share, color = themeColorState.buttonColor, hideText = hideButtonTextProvider(), text = stringResource(R.string.share))
        }
    }
}

@Composable
private fun RowScope.IconicsButtonContent(
    iIcon: IIcon,
    color: Color = MaterialTheme.colorScheme.primary,
    text: String,
    hideText: Boolean,
    iconicsSize: Dp = 24.dp,
) {
    Image(asset = iIcon, contentDescription = null, modifier = Modifier.size(iconicsSize), colorFilter = ColorFilter.tint(color = color))
    if (!hideText) {
        ButtonText(text = text, color = color)
    }
}

@Composable
private fun RowScope.ButtonContent(
    icon: ImageVector,
    text: String,
    hideText: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
    if (!hideText) {
        ButtonText(text = text, color = color)
    }
}

@Composable
private fun RowScope.ButtonText(text: String, color: Color) {
    if (text.isNotEmpty()) {
        Gap(8.dp)
        Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(color = color.copy(alpha = .8f), letterSpacing = (-.5).sp, fontWeight = FontWeight.Medium))
    }
}

private data class ButtonConfig(val icon: ImageVector? = null, val iIcon: IIcon? = null, val buttonColors: ButtonColors, val borderStroke: BorderStroke, val text: String)
