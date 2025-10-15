package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.components.icons.AccountTreeIcon
import org.nekomanga.presentation.components.icons.ArtTrackIcon
import org.nekomanga.presentation.components.icons.CheckDecagramIcon
import org.nekomanga.presentation.components.icons.Numeric0BoxOutlineIcon
import org.nekomanga.presentation.components.icons.Numeric1BoxOutlineIcon
import org.nekomanga.presentation.components.icons.Numeric2BoxOutlineIcon
import org.nekomanga.presentation.components.icons.Numeric3BoxOutlineIcon
import org.nekomanga.presentation.components.icons.Numeric4BoxOutlineIcon
import org.nekomanga.presentation.components.icons.SourceMergeIcon
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

/** Block of buttons for the actions on the backdrop screen */
@Composable
fun ButtonBlock(
    hideButtonText: Boolean,
    isInitialized: Boolean,
    isMerged: Boolean,
    inLibrary: Boolean,
    loggedIntoTrackers: Boolean,
    trackServiceCount: Int,
    themeColorState: ThemeColorState,
    toggleFavorite: () -> Unit,
    trackingClick: () -> Unit,
    artworkClick: () -> Unit,
    similarClick: () -> Unit,
    mergeClick: () -> Unit,
    linksClick: () -> Unit,
    shareClick: () -> Unit,
    moveCategories: () -> Unit,
) {
    if (!isInitialized) return

    val checkedButtonColors =
        ButtonDefaults.outlinedButtonColors(containerColor = themeColorState.containerColor)
    val checkedBorderStroke = BorderStroke(Size.extraExtraTiny, Color.Transparent)
    val uncheckedButtonColors = ButtonDefaults.outlinedButtonColors()
    val uncheckedBorderStroke =
        BorderStroke(
            Size.extraExtraTiny,
            themeColorState.containerColor.copy(alpha = NekoColors.mediumAlphaHighContrast),
        )

    val (padding, buttonModifier) =
        remember(hideButtonText) {
            if (hideButtonText) {
                PaddingValues(Size.none) to Modifier.size(Size.huge)
            } else {
                PaddingValues(horizontal = Size.smedium, vertical = Size.small) to
                    Modifier.height(Size.huge)
            }
        }

    val actionButtons =
        remember(inLibrary, isMerged, trackServiceCount, loggedIntoTrackers) {
            persistentListOf<ActionButtonData>()
                .builder()
                .apply {
                    // Favorite Button
                    add(
                        ActionButtonData(
                            icon =
                                if (inLibrary) Icons.Filled.Favorite
                                else Icons.Filled.FavoriteBorder,
                            text = UiText.String(""),
                            isChecked = inLibrary,
                            onClick = toggleFavorite,
                            dropdownItems =
                                if (inLibrary) {
                                    persistentListOf(
                                        SimpleDropDownItem.Action(
                                            text =
                                                UiText.StringResource(R.string.remove_from_library),
                                            onClick = toggleFavorite,
                                        ),
                                        SimpleDropDownItem.Action(
                                            text = UiText.StringResource(R.string.edit_categories),
                                            onClick = moveCategories,
                                        ),
                                    )
                                } else {
                                    null
                                },
                        )
                    )

                    // Tracking Button (conditionally added)
                    if (loggedIntoTrackers) {
                        val isTracked = trackServiceCount > 0
                        val trackerIcon =
                            when {
                                isTracked && hideButtonText ->
                                    when (trackServiceCount) {
                                        1 -> Numeric1BoxOutlineIcon
                                        2 -> Numeric2BoxOutlineIcon
                                        3 -> Numeric3BoxOutlineIcon
                                        4 -> Numeric4BoxOutlineIcon
                                        else -> Numeric0BoxOutlineIcon
                                    }
                                isTracked -> Icons.Filled.Check
                                else -> Icons.Filled.Sync
                            }
                        add(
                            ActionButtonData(
                                icon = trackerIcon,
                                text =
                                    if (isTracked)
                                        UiText.StringResource(R.string._tracked, trackServiceCount)
                                    else UiText.StringResource(R.string.tracking),
                                isChecked = isTracked,
                                onClick = trackingClick,
                            )
                        )
                    }

                    // Other buttons
                    add(
                        ActionButtonData(
                            icon = ArtTrackIcon,
                            text = UiText.StringResource(R.string.artwork),
                            onClick = artworkClick,
                        )
                    )
                    add(
                        ActionButtonData(
                            icon = AccountTreeIcon,
                            text = UiText.StringResource(R.string.similar_work),
                            onClick = similarClick,
                        )
                    )
                    add(
                        ActionButtonData(
                            icon = if (isMerged) CheckDecagramIcon else SourceMergeIcon,
                            text =
                                UiText.StringResource(
                                    if (isMerged) R.string.is_merged else R.string.is_not_merged
                                ),
                            isChecked = isMerged,
                            onClick = mergeClick,
                        )
                    )
                    add(
                        ActionButtonData(
                            icon = Icons.Filled.OpenInBrowser,
                            text = UiText.StringResource(R.string.links),
                            onClick = linksClick,
                        )
                    )
                    add(
                        ActionButtonData(
                            icon = Icons.Filled.Share,
                            text = UiText.StringResource(R.string.share),
                            onClick = shareClick,
                        )
                    )
                }
                .build()
        }

    // The UI is rendered by iterating over the data list.
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Size.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        actionButtons.forEach { data ->
            // Use a key for performance and state correctness in loops
            key(data.text) {
                ActionButton(
                    data = data,
                    modifier = buttonModifier,
                    contentPadding = padding,
                    colors = if (data.isChecked) checkedButtonColors else uncheckedButtonColors,
                    border = if (data.isChecked) checkedBorderStroke else uncheckedBorderStroke,
                    hideText = hideButtonText,
                    themeColorState = themeColorState,
                )
            }
        }
    }
}

/** A generic, reusable composable that renders a single button based on ActionButtonData. */
@Composable
private fun ActionButton(
    data: ActionButtonData,
    modifier: Modifier,
    contentPadding: PaddingValues,
    colors: ButtonColors,
    border: BorderStroke,
    hideText: Boolean,
    themeColorState: ThemeColorState,
) {
    var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

    val finalOnClick = {
        if (data.dropdownItems == null) {
            data.onClick()
        } else {
            // For favorite button, a normal click opens the dropdown when in library
            if (!data.isChecked) {
                data.onClick()
            } else {
                favoriteExpanded = true
            }
        }
    }

    OutlinedButton(
        onClick = finalOnClick,
        modifier = if (data.dropdownItems != null) Modifier.size(Size.huge) else modifier,
        shapes = ButtonDefaults.shapes(),
        colors = colors,
        border = border,
        contentPadding =
            if (data.dropdownItems != null) PaddingValues(Size.none) else contentPadding,
    ) {
        Box {
            Row {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    modifier = Modifier.size(Size.large),
                    tint = themeColorState.primaryColor,
                )
                if (!hideText) {
                    val text = data.text.asString()
                    if (text.isNotEmpty()) {
                        Gap(Size.tiny)
                        Text(
                            text = text,
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = themeColorState.primaryColor.copy(alpha = .8f),
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                    }
                }
            }
        }

        if (data.dropdownItems != null) {
            SimpleDropdownMenu(
                expanded = favoriteExpanded,
                themeColorState = themeColorState,
                onDismiss = { favoriteExpanded = false },
                dropDownItems =
                    data.dropdownItems
                        .map {
                            if (it is SimpleDropDownItem.Action) {
                                it.copy(
                                    onClick = {
                                        it.onClick()
                                        favoriteExpanded = false
                                    }
                                )
                            } else {
                                it
                            }
                        }
                        .toPersistentList(),
            )
        }
    }
}

private data class ActionButtonData(
    val icon: ImageVector,
    val text: UiText,
    val onClick: () -> Unit,
    val isChecked: Boolean = false,
    val dropdownItems: ImmutableList<SimpleDropDownItem>? = null,
)
