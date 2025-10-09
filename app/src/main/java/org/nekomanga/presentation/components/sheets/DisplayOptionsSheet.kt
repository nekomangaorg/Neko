package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.util.system.isLandscape
import jp.wasabeef.gap.Gap
import kotlin.math.roundToInt
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun DisplayOptionsSheet(
    currentLibraryDisplayMode: LibraryDisplayMode,
    libraryDisplayModeClick: (LibraryDisplayMode) -> Unit,
    rawColumnCount: Float,
    rawColumnCountChanged: (Float) -> Unit,
    outlineCoversEnabled: Boolean,
    outlineCoversToggled: () -> Unit,
    unreadBadgesEnabled: Boolean,
    unreadBadgesToggled: () -> Unit,
    downloadBadgesEnabled: Boolean,
    downloadBadgesToggled: () -> Unit,
    showStartReadingButtonEnabled: Boolean,
    startReadingButtonToggled: () -> Unit,
    horizontalCategoriesEnabled: Boolean,
    horizontalCategoriesToggled: () -> Unit,
    showLibraryButtonBarEnabled: Boolean,
    showLibraryButtonBarToggled: () -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    bottomContentPadding: Dp = Size.medium,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp

        BaseSheet(
            themeColor = themeColorState,
            maxSheetHeightPercentage = .9f,
            bottomPaddingAroundContent = bottomContentPadding,
        ) {
            val paddingModifier = Modifier.padding(horizontal = Size.small)

            Gap(Size.small)
            Text(
                modifier = paddingModifier.fillMaxWidth(),
                text = stringResource(R.string.display_options),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Gap(Size.large)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Size.medium),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LibraryDisplayMode.entries().forEachIndexed { index, libraryDisplayMode ->
                        ToggleButton(
                            modifier =
                                Modifier.weight(if (index == 1) 1.25f else 1f).semantics {
                                    role = Role.RadioButton
                                },
                            checked = libraryDisplayMode == currentLibraryDisplayMode,
                            onCheckedChange = { libraryDisplayModeClick(libraryDisplayMode) },
                            shapes =
                                when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    LibraryDisplayMode.entries().lastIndex ->
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                        ) {
                            Text(libraryDisplayMode.toUiText().asString())
                        }
                        Gap(ButtonGroupDefaults.ConnectedSpaceBetween)
                    }
                }

                if (currentLibraryDisplayMode != LibraryDisplayMode.List) {
                    var sliderPosition by rememberSaveable {
                        mutableFloatStateOf(((rawColumnCount + .5f) * 2f).roundToInt().toFloat())
                    }

                    Column(modifier = Modifier.padding(horizontal = Size.medium)) {
                        val context = LocalContext.current
                        val isPortrait = !context.isLandscape()
                        val numberOfColumns =
                            numberOfColumns(rawValue = sliderPosition, forText = true)
                        val numberOfColumnsAlt =
                            numberOfColumns(
                                rawValue = sliderPosition,
                                forText = true,
                                useHeight = true,
                            )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text =
                                "Grid size: Portrait: ${if (isPortrait) numberOfColumns else numberOfColumnsAlt} ${Constants.SEPARATOR} Landscape: ${if (isPortrait) numberOfColumnsAlt else numberOfColumns}",
                        )
                        Gap(Size.tiny)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                modifier = Modifier.weight(1f),
                                value = sliderPosition,
                                onValueChange = { sliderPosition = it },
                                onValueChangeFinished = {
                                    rawColumnCountChanged((sliderPosition / 2f) - .5f)
                                },
                                steps = 8,
                                valueRange = 0f..7f,
                            )
                            Gap(Size.tiny)
                            TextButton(
                                onClick = {
                                    sliderPosition = 3f
                                    rawColumnCountChanged((sliderPosition / 2f) - .5f)
                                },
                                shapes = ButtonDefaults.shapes(),
                            ) {
                                Text(stringResource(R.string.reset))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = outlineCoversToggled),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Gap(Size.small)
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.show_outline_around_covers),
                    )
                    Switch(
                        checked = outlineCoversEnabled,
                        onCheckedChange = { outlineCoversToggled() },
                    )
                    Gap(Size.small)
                }

                HorizontalDivider()

                ToggleRow(
                    enabled = unreadBadgesEnabled,
                    onClick = unreadBadgesToggled,
                    text = stringResource(id = R.string.unread_badge),
                )

                ToggleRow(
                    enabled = downloadBadgesEnabled,
                    onClick = downloadBadgesToggled,
                    text = stringResource(R.string.download_badge),
                )

                ToggleRow(
                    enabled = showStartReadingButtonEnabled,
                    onClick = startReadingButtonToggled,
                    text = stringResource(R.string.show_start_reading_button),
                )
                HorizontalDivider()
                ToggleRow(
                    enabled = showLibraryButtonBarEnabled,
                    onClick = showLibraryButtonBarToggled,
                    text = stringResource(R.string.show_library_action_bar),
                )

                ToggleRow(
                    enabled = horizontalCategoriesEnabled,
                    onClick = horizontalCategoriesToggled,
                    text = stringResource(R.string.horizontal_categories),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(enabled: Boolean, onClick: () -> Unit, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Gap(Size.small)
        Text(modifier = Modifier.weight(1f), text = text)
        Switch(checked = enabled, onCheckedChange = { onClick() })
        Gap(Size.small)
    }
}
