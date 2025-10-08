package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

/** Block of buttons for the actions on the backdrop screen */
@Composable
fun ButtonBlock(
    hideButtonTextProvider: () -> Boolean,
    isInitializedProvider: () -> Boolean,
    isMergedProvider: () -> Boolean,
    inLibraryProvider: () -> Boolean,
    loggedIntoTrackersProvider: () -> Boolean,
    trackServiceCountProvider: () -> Int,
    themeColorState: ThemeColorState,
    toggleFavorite: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
    moveCategories: () -> Unit = {},
) {
    if (!isInitializedProvider()) {
        return
    }

    var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

    val checkedButtonColors =
        ButtonDefaults.outlinedButtonColors(containerColor = themeColorState.altContainerColor)
    val checkedBorderStroke = BorderStroke(Size.extraExtraTiny, Color.Transparent)

    val uncheckedButtonColors = ButtonDefaults.outlinedButtonColors()
    val uncheckedBorderStroke =
        BorderStroke(
            Size.extraExtraTiny,
            themeColorState.altContainerColor.copy(alpha = NekoColors.mediumAlphaHighContrast),
        )
    val (padding, iconicsPadding, buttonModifier) =
        when (hideButtonTextProvider()) {
            true ->
                Triple(PaddingValues(Size.none), PaddingValues(Size.none), Modifier.size(Size.huge))
            false ->
                Triple(
                    PaddingValues(horizontal = 12.dp, vertical = Size.small),
                    PaddingValues(horizontal = 12.dp, vertical = Size.tiny),
                    Modifier.height(Size.huge),
                )
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Size.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        val favConfig =
            when (inLibraryProvider()) {
                true ->
                    ButtonConfig(
                        icon = Icons.Filled.Favorite,
                        buttonColors = checkedButtonColors,
                        borderStroke = checkedBorderStroke,
                        text = stringResource(R.string.in_library),
                    )
                false ->
                    ButtonConfig(
                        icon = Icons.Filled.FavoriteBorder,
                        buttonColors = uncheckedButtonColors,
                        borderStroke = uncheckedBorderStroke,
                        text = stringResource(R.string.add_to_library),
                    )
            }

        OutlinedButton(
            shapes = ButtonDefaults.shapes(),
            colors = favConfig.buttonColors,
            modifier = Modifier.size(Size.huge),
            onClick = {
                if (!inLibraryProvider()) {
                    toggleFavorite()
                } else {
                    favoriteExpanded = true
                }
            },
            border = favConfig.borderStroke,
            contentPadding = PaddingValues(Size.none),
        ) {
            Icon(
                imageVector = favConfig.icon!!,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = themeColorState.buttonColor,
            )
            SimpleDropdownMenu(
                expanded = favoriteExpanded,
                themeColorState = themeColorState,
                onDismiss = { favoriteExpanded = false },
                dropDownItems =
                    persistentListOf(
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.remove_from_library),
                            onClick = {
                                toggleFavorite()
                                favoriteExpanded = false
                            },
                        ),
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.edit_categories),
                            onClick = {
                                moveCategories()
                                favoriteExpanded = false
                            },
                        ),
                    ),
            )
        }

        if (loggedIntoTrackersProvider()) {

            val trackerConfig =
                when {
                    trackServiceCountProvider() > 0 ->
                        ButtonConfig(
                            icon = Icons.Filled.Check,
                            buttonColors = checkedButtonColors,
                            borderStroke = checkedBorderStroke,
                            text = stringResource(R.string._tracked, trackServiceCountProvider()),
                        )
                    else ->
                        ButtonConfig(
                            icon = Icons.Filled.Sync,
                            buttonColors = uncheckedButtonColors,
                            borderStroke = uncheckedBorderStroke,
                            text = stringResource(R.string.tracking),
                        )
                }

            OutlinedButton(
                onClick = trackingClick,
                modifier = buttonModifier,
                shapes = ButtonDefaults.shapes(),
                colors = trackerConfig.buttonColors,
                border = trackerConfig.borderStroke,
                contentPadding = padding,
            ) {
                if (trackServiceCountProvider() > 0 && hideButtonTextProvider()) {
                    val icon =
                        when (trackServiceCountProvider()) {
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
                    ButtonContent(
                        trackerConfig.icon!!,
                        color = themeColorState.buttonColor,
                        hideText = hideButtonTextProvider(),
                        text = trackerConfig.text,
                    )
                }
            }
        }

        OutlinedButton(
            shapes = ButtonDefaults.shapes(),
            onClick = artworkClick,
            modifier = buttonModifier,
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

        OutlinedButton(
            shapes = ButtonDefaults.shapes(),
            onClick = similarClick,
            modifier = buttonModifier,
            border = uncheckedBorderStroke,
            contentPadding = padding,
        ) {
            ButtonContent(
                Icons.Filled.AccountTree,
                color = themeColorState.buttonColor,
                hideText = hideButtonTextProvider(),
                text = stringResource(R.string.similar_work),
            )
        }

        val mergeConfig =
            when (isMergedProvider()) {
                true ->
                    ButtonConfig(
                        iIcon = CommunityMaterial.Icon.cmd_check_decagram,
                        buttonColors = checkedButtonColors,
                        borderStroke = checkedBorderStroke,
                        text = stringResource(R.string.is_merged),
                    )
                false ->
                    ButtonConfig(
                        iIcon = CommunityMaterial.Icon3.cmd_source_merge,
                        buttonColors = uncheckedButtonColors,
                        borderStroke = uncheckedBorderStroke,
                        text = stringResource(R.string.is_not_merged),
                    )
            }

        OutlinedButton(
            shapes = ButtonDefaults.shapes(),
            onClick = mergeClick,
            modifier = buttonModifier,
            colors = mergeConfig.buttonColors,
            border = mergeConfig.borderStroke,
            contentPadding = iconicsPadding,
        ) {
            IconicsButtonContent(
                iIcon = mergeConfig.iIcon!!,
                color = themeColorState.buttonColor,
                hideText = hideButtonTextProvider(),
                text = mergeConfig.text,
                iconicsSize = 28.dp,
            )
        }

        OutlinedButton(
            shapes = ButtonDefaults.shapes(),
            onClick = linksClick,
            modifier = buttonModifier,
            border = uncheckedBorderStroke,
            contentPadding = padding,
        ) {
            ButtonContent(
                icon = Icons.Filled.OpenInBrowser,
                color = themeColorState.buttonColor,
                hideText = hideButtonTextProvider(),
                text = stringResource(R.string.links),
            )
        }

        OutlinedButton(
            shapes = ButtonDefaults.shapes(),
            onClick = shareClick,
            modifier = buttonModifier,
            border = uncheckedBorderStroke,
            contentPadding = padding,
        ) {
            ButtonContent(
                icon = Icons.Filled.Share,
                color = themeColorState.buttonColor,
                hideText = hideButtonTextProvider(),
                text = stringResource(R.string.share),
            )
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
    Image(
        asset = iIcon,
        contentDescription = null,
        modifier = Modifier.size(iconicsSize),
        colorFilter = ColorFilter.tint(color = color),
    )
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
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = color,
    )
    if (!hideText) {
        ButtonText(text = text, color = color)
    }
}

@Composable
private fun RowScope.ButtonText(text: String, color: Color) {
    if (text.isNotEmpty()) {
        Gap(Size.tiny)
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = color.copy(alpha = .8f),
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

private data class ButtonConfig(
    val icon: ImageVector? = null,
    val iIcon: IIcon? = null,
    val buttonColors: ButtonColors,
    val borderStroke: BorderStroke,
    val text: String,
)
