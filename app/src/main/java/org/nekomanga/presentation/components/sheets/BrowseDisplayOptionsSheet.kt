package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.browse.LibraryEntryVisibility
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun BrowseDisplayOptionsSheet(
    modifier: Modifier = Modifier,
    showIsList: Boolean = false,
    isList: Boolean,
    switchDisplayClick: () -> Unit,
    currentLibraryEntryVisibility: Int,
    libraryEntryVisibilityClick: (Int) -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    bottomContentPadding: Dp = Size.medium,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .9

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
            LazyColumn(
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp),
                verticalArrangement = Arrangement.spacedBy(Size.medium),
            ) {
                if (showIsList) {
                    item {
                        Text(
                            text = stringResource(R.string.display_as),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                        )
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ToggleButton(
                                checked = isList,
                                onCheckedChange = { switchDisplayClick() },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            ) {
                                Text(stringResource(R.string.list))
                            }
                            Gap(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ToggleButton(
                                checked = !isList,
                                onCheckedChange = { switchDisplayClick() },
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            ) {
                                Text(stringResource(R.string.grid))
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.filter_results),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                    )
                }

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Size.medium),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ToggleButton(
                            checked =
                                currentLibraryEntryVisibility ==
                                    LibraryEntryVisibility.SHOW_NOT_IN_LIBRARY,
                            onCheckedChange = {
                                libraryEntryVisibilityClick(
                                    LibraryEntryVisibility.SHOW_NOT_IN_LIBRARY
                                )
                            },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        ) {
                            Text(stringResource(R.string.hide_library_manga))
                        }
                        Gap(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ToggleButton(
                            checked =
                                currentLibraryEntryVisibility == LibraryEntryVisibility.SHOW_ALL,
                            onCheckedChange = {
                                libraryEntryVisibilityClick(LibraryEntryVisibility.SHOW_ALL)
                            },
                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                        ) {
                            Text(stringResource(R.string.show_all_manga))
                        }
                        Gap(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ToggleButton(
                            checked =
                                currentLibraryEntryVisibility ==
                                    LibraryEntryVisibility.SHOW_IN_LIBRARY,
                            onCheckedChange = {
                                libraryEntryVisibilityClick(LibraryEntryVisibility.SHOW_IN_LIBRARY)
                            },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        ) {
                            Text(stringResource(R.string.show_library_manga))
                        }
                    }
                }
            }
        }
    }
}
