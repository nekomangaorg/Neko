package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderSliderPosition
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderSettingsSheet(
    uiState: ReaderSettingsUiState,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    onAction: (ReaderSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tabs =
        listOf(
            stringResource(R.string.general),
            if (uiState.isWebtoon) stringResource(R.string.webtoon)
            else stringResource(R.string.paged),
            stringResource(R.string.filter),
        )

    val pagerState = rememberPagerState(initialPage = uiState.selectedTab, pageCount = { 3 })
    val maxLazyHeight = LocalConfiguration.current.screenHeightDp * 0.6f

    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = 0.9f,
        bottomPaddingAroundContent = Size.none,
    ) {
        Column(
            modifier =
                modifier.fillMaxWidth().padding(horizontal = Size.medium, vertical = Size.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Size.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(
                                pagerState.currentPage,
                                matchContentSize = true,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight =
                                        if (pagerState.currentPage == index) FontWeight.Bold
                                        else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }

                IconButton(
                    onClick = { onAction(ReaderSettingsAction.OpenFullSettings) },
                    modifier = Modifier.padding(start = Size.small),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_settings_24dp),
                        contentDescription = stringResource(R.string.reader_settings),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp),
            ) { page ->
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (page) {
                        0 -> GeneralSettingsTab(uiState = uiState, onAction = onAction)
                        1 -> LayoutSettingsTab(uiState = uiState, onAction = onAction)
                        2 -> FilterSettingsTab(uiState = uiState, onAction = onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    uiState: ReaderSettingsUiState,
    onAction: (ReaderSettingsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val readingModeOptions = stringArrayResource(id = R.array.viewers_selector).toList()
        ReaderChipsSelector(
            label = stringResource(R.string.reading_mode),
            options = readingModeOptions,
            selectedIndex = uiState.readingModeIndex,
            onSelected = { index ->
                val readingModeType = ReadingModeType.fromSpinner(index)
                onAction(ReaderSettingsAction.SetReadingMode(readingModeType))
            },
        )

        val rotationOptions = stringArrayResource(id = R.array.rotation_type).toList()
        ReaderChipsSelector(
            label = stringResource(R.string.rotation),
            options = rotationOptions,
            selectedIndex = uiState.rotationIndex,
            onSelected = { index ->
                val rotationType = OrientationType.fromSpinner(index)
                onAction(ReaderSettingsAction.SetOrientation(rotationType))
            },
        )

        val themeOptions = stringArrayResource(id = R.array.reader_themes).toList()
        ReaderChipsSelector(
            label = stringResource(R.string.background_color),
            options = themeOptions,
            selectedIndex = uiState.readerTheme,
            onSelected = { index -> onAction(ReaderSettingsAction.SetTheme(index)) },
        )

        val sliderOptions = stringArrayResource(id = R.array.slider_position).toList()
        ReaderChipsSelector(
            label = stringResource(R.string.slider_position),
            options = sliderOptions,
            selectedIndex = uiState.sliderPosition.prefValue,
            onSelected = { index ->
                onAction(
                    ReaderSettingsAction.SetSliderPosition(ReaderSliderPosition.fromSpinner(index))
                )
            },
        )

        ReaderSwitchSetting(
            label = stringResource(R.string.show_page_number),
            checked = uiState.showPageNumber,
            onCheckedChange = { onAction(ReaderSettingsAction.ToggleShowPageNumber(it)) },
        )

        ReaderSwitchSetting(
            label = stringResource(R.string.keep_screen_on),
            checked = uiState.keepScreenOn,
            onCheckedChange = { onAction(ReaderSettingsAction.ToggleKeepScreenOn(it)) },
        )

        ReaderSwitchSetting(
            label = stringResource(R.string.always_show_chapter_transition),
            checked = uiState.alwaysShowChapterTransition,
            onCheckedChange = {
                onAction(ReaderSettingsAction.ToggleAlwaysShowChapterTransition(it))
            },
        )
    }
}

@Composable
private fun LayoutSettingsTab(
    uiState: ReaderSettingsUiState,
    onAction: (ReaderSettingsAction) -> Unit,
) {
    if (uiState.isWebtoon) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ReaderSwitchSetting(
                label = stringResource(R.string.crop_borders),
                checked = uiState.effectiveCropBorders,
                onCheckedChange = {
                    if (uiState.hasMargins) {
                        onAction(ReaderSettingsAction.ToggleCropBorders(it))
                    } else {
                        onAction(ReaderSettingsAction.ToggleWebtoonCropBorders(it))
                    }
                },
            )

            ReaderSliderSetting(
                label = stringResource(R.string.pref_webtoon_side_padding),
                value = uiState.webtoonSidePadding.toFloat().coerceIn(0f, 25f),
                valueRange = 0f..25f,
                valueFormatter = { "${it.roundToInt()}%" },
                onValueChange = {
                    onAction(ReaderSettingsAction.SetWebtoonSidePadding(it.roundToInt()))
                },
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.enable_zoom_out),
                checked = uiState.webtoonEnableZoomOut,
                onCheckedChange = { onAction(ReaderSettingsAction.ToggleWebtoonEnableZoomOut(it)) },
            )

            val webtoonNavOptions = stringArrayResource(id = R.array.reader_nav).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.tap_zones),
                options = webtoonNavOptions,
                selectedIndex = uiState.webtoonNav,
                onSelected = { onAction(ReaderSettingsAction.SetWebtoonNav(it)) },
            )

            val invertTappingOptions =
                stringArrayResource(id = R.array.invert_tapping_mode).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.invert_tapping),
                options = invertTappingOptions,
                selectedIndex = uiState.webtoonInvert.ordinal,
                onSelected = { index ->
                    onAction(
                        ReaderSettingsAction.SetWebtoonInvertTapping(
                            ViewerNavigation.TappingInvertMode.values()[index]
                        )
                    )
                },
            )

            val webtoonPageLayoutOptions =
                stringArrayResource(id = R.array.webtoon_page_layouts).toList()
            val selectedWebtoonLayoutIndex =
                if (uiState.webtoonPageLayout == PageLayout.SPLIT_PAGES) 1 else 0
            ReaderChipsSelector(
                label = stringResource(R.string.page_layout),
                options = webtoonPageLayoutOptions,
                selectedIndex = selectedWebtoonLayoutIndex,
                onSelected = { index ->
                    val layout = if (index == 1) PageLayout.SPLIT_PAGES else PageLayout.SINGLE_PAGE
                    onAction(ReaderSettingsAction.SetWebtoonPageLayout(layout))
                },
            )

            if (uiState.webtoonPageLayout != PageLayout.SINGLE_PAGE) {
                ReaderSwitchSetting(
                    label = stringResource(R.string.invert_double_pages),
                    checked = uiState.webtoonInvertDoublePages,
                    onCheckedChange = {
                        onAction(ReaderSettingsAction.ToggleWebtoonInvertDoublePages(it))
                    },
                )
            }

            ReaderSwitchSetting(
                label = stringResource(R.string.animate_page_transitions_webtoon),
                checked = uiState.webtoonPageTransitions,
                onCheckedChange = {
                    onAction(ReaderSettingsAction.ToggleWebtoonPageTransitions(it))
                },
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.split_tall_images_reader),
                checked = uiState.splitTallImages,
                onCheckedChange = { onAction(ReaderSettingsAction.ToggleSplitTallImages(it)) },
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            val scaleOptions = stringArrayResource(id = R.array.image_scale_type).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.scale_type),
                options = scaleOptions,
                selectedIndex = uiState.imageScaleType - 1,
                onSelected = { index ->
                    onAction(ReaderSettingsAction.SetImageScaleType(index + 1))
                },
            )

            val zoomStartOptions = stringArrayResource(id = R.array.zoom_start).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.zoom_start_position),
                options = zoomStartOptions,
                selectedIndex = uiState.zoomStart - 1,
                onSelected = { index -> onAction(ReaderSettingsAction.SetZoomStart(index + 1)) },
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.crop_borders),
                checked = uiState.cropBorders,
                onCheckedChange = { onAction(ReaderSettingsAction.ToggleCropBorders(it)) },
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.animate_page_transitions),
                checked = uiState.pageTransitions,
                onCheckedChange = { onAction(ReaderSettingsAction.TogglePageTransitions(it)) },
            )

            val pagerNavOptions = stringArrayResource(id = R.array.reader_nav).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.tap_zones),
                options = pagerNavOptions,
                selectedIndex = uiState.pagerNav,
                onSelected = { onAction(ReaderSettingsAction.SetPagerNav(it)) },
            )

            val invertTappingOptions =
                stringArrayResource(id = R.array.invert_tapping_mode).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.invert_tapping),
                options = invertTappingOptions,
                selectedIndex = uiState.pagerInvert.ordinal,
                onSelected = { index ->
                    onAction(
                        ReaderSettingsAction.SetPagerInvertTapping(
                            ViewerNavigation.TappingInvertMode.values()[index]
                        )
                    )
                },
            )

            val pagedLayoutOptions = stringArrayResource(id = R.array.page_layouts).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.page_layout),
                options = pagedLayoutOptions,
                selectedIndex = uiState.pageLayout.value,
                onSelected = { index ->
                    onAction(ReaderSettingsAction.SetPageLayout(PageLayout.fromPreference(index)))
                },
            )

            if (uiState.pageLayout != PageLayout.SINGLE_PAGE) {
                ReaderSwitchSetting(
                    label = stringResource(R.string.invert_double_pages),
                    checked = uiState.invertDoublePages,
                    onCheckedChange = {
                        onAction(ReaderSettingsAction.ToggleInvertDoublePages(it))
                    },
                )

                val gapOptions = stringArrayResource(id = R.array.double_page_gap).toList()
                val selectedGapIndex =
                    maxOf(0, gapOptions.indexOf(uiState.doublePageGap.toString()))
                ReaderChipsSelector(
                    label = stringResource(R.string.double_page_gap),
                    options = gapOptions,
                    selectedIndex = selectedGapIndex,
                    onSelected = { index ->
                        onAction(ReaderSettingsAction.SetDoublePageGap(gapOptions[index].toInt()))
                    },
                )
            }

            if (uiState.imageScaleType == 1) { // Fit screen / Center Inside
                ReaderSwitchSetting(
                    label = stringResource(R.string.zoom_double_page_spreads),
                    checked = uiState.landscapeZoom,
                    onCheckedChange = {
                        onAction(ReaderSettingsAction.ToggleZoomDoublePageSpreads(it))
                    },
                )
            }

            ReaderSwitchSetting(
                label = stringResource(R.string.navigate_pan),
                checked = uiState.navigateToPan,
                onCheckedChange = { onAction(ReaderSettingsAction.ToggleNavigateToPan(it)) },
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.double_page_rotate),
                checked = uiState.doublePageRotate,
                onCheckedChange = { onAction(ReaderSettingsAction.ToggleDoublePageRotate(it)) },
            )

            if (uiState.doublePageRotate) {
                ReaderSwitchSetting(
                    label = stringResource(R.string.double_page_rotate_reverse),
                    checked = uiState.doublePageRotateReverse,
                    onCheckedChange = {
                        onAction(ReaderSettingsAction.ToggleDoublePageRotateReverse(it))
                    },
                )
            }

            if (uiState.isCutoutVisible) {
                val cutoutOptions = stringArrayResource(id = R.array.cutout_behavior).toList()
                ReaderChipsSelector(
                    label = stringResource(R.string.cutout_area_behavior),
                    options = cutoutOptions,
                    selectedIndex = uiState.pagerCutoutBehavior,
                    onSelected = { onAction(ReaderSettingsAction.SetPagerCutoutBehavior(it)) },
                )
            }
        }
    }
}

@Composable
private fun FilterSettingsTab(
    uiState: ReaderSettingsUiState,
    onAction: (ReaderSettingsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ReaderSwitchSetting(
            label = stringResource(R.string.grayscale),
            checked = uiState.grayscale,
            onCheckedChange = { onAction(ReaderSettingsAction.ToggleGrayscale(it)) },
        )

        ReaderSwitchSetting(
            label = stringResource(R.string.pref_inverted_colors),
            checked = uiState.invertedColors,
            onCheckedChange = { onAction(ReaderSettingsAction.ToggleInvertedColors(it)) },
        )

        ReaderSwitchSetting(
            label = stringResource(R.string.use_custom_brightness),
            checked = uiState.customBrightness,
            onCheckedChange = { onAction(ReaderSettingsAction.ToggleCustomBrightness(it)) },
        )

        if (uiState.customBrightness) {
            ReaderSliderSetting(
                label = stringResource(R.string.use_custom_brightness),
                value = uiState.customBrightnessValue.toFloat(),
                valueRange = -75f..100f,
                valueFormatter = { it.roundToInt().toString() },
                onValueChange = {
                    onAction(ReaderSettingsAction.SetCustomBrightnessValue(it.roundToInt()))
                },
            )
        }

        ReaderSwitchSetting(
            label = stringResource(R.string.use_custom_color_filter),
            checked = uiState.colorFilter,
            onCheckedChange = { onAction(ReaderSettingsAction.ToggleColorFilter(it)) },
        )

        if (uiState.colorFilter) {
            val blendModes = stringArrayResource(id = R.array.color_filter_modes).toList()
            ReaderChipsSelector(
                label = stringResource(R.string.color_filter_blend_mode),
                options = blendModes,
                selectedIndex = uiState.colorFilterMode,
                onSelected = { onAction(ReaderSettingsAction.SetColorFilterMode(it)) },
            )

            ReaderSliderSetting(
                label = stringResource(R.string.alpha_initial),
                value = uiState.colorFilterAlpha.toFloat(),
                valueRange = 0f..255f,
                valueFormatter = { it.roundToInt().toString() },
                onValueChange = { value ->
                    onAction(
                        ReaderSettingsAction.SetColorFilterValue(
                            uiState.withColorComponent(
                                value.roundToInt(),
                                COLOR_FILTER_ALPHA_MASK,
                                24,
                            )
                        )
                    )
                },
            )

            ReaderSliderSetting(
                label = stringResource(R.string.red_initial),
                value = uiState.colorFilterRed.toFloat(),
                valueRange = 0f..255f,
                valueFormatter = { it.roundToInt().toString() },
                onValueChange = { value ->
                    onAction(
                        ReaderSettingsAction.SetColorFilterValue(
                            uiState.withColorComponent(
                                value.roundToInt(),
                                COLOR_FILTER_RED_MASK,
                                16,
                            )
                        )
                    )
                },
            )

            ReaderSliderSetting(
                label = stringResource(R.string.green_initial),
                value = uiState.colorFilterGreen.toFloat(),
                valueRange = 0f..255f,
                valueFormatter = { it.roundToInt().toString() },
                onValueChange = { value ->
                    onAction(
                        ReaderSettingsAction.SetColorFilterValue(
                            uiState.withColorComponent(
                                value.roundToInt(),
                                COLOR_FILTER_GREEN_MASK,
                                8,
                            )
                        )
                    )
                },
            )

            ReaderSliderSetting(
                label = stringResource(R.string.blue_initial),
                value = uiState.colorFilterBlue.toFloat(),
                valueRange = 0f..255f,
                valueFormatter = { it.roundToInt().toString() },
                onValueChange = { value ->
                    onAction(
                        ReaderSettingsAction.SetColorFilterValue(
                            uiState.withColorComponent(
                                value.roundToInt(),
                                COLOR_FILTER_BLUE_MASK,
                                0,
                            )
                        )
                    )
                },
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = Size.smedium),
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderChipsSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = Size.small)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Size.tiny),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Size.small),
            verticalArrangement = Arrangement.spacedBy(Size.small),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                FilterChip(
                    selected = selected,
                    onClick = { onSelected(index) },
                    label = { Text(option) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = selected,
                        ),
                )
            }
        }
    }
}

@Composable
fun ReaderSwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = Size.smedium),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun ReaderSliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = Size.small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors =
                SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    thumbColor = MaterialTheme.colorScheme.primary,
                ),
            modifier = Modifier.fillMaxWidth().padding(top = Size.tiny),
        )
    }
}

@Preview
@Composable
private fun ReaderSettingsSheetPagedPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        ReaderSettingsSheet(
            uiState =
                ReaderSettingsUiState(
                    isWebtoon = false,
                    readingMode = ReadingModeType.DEFAULT,
                    orientation = OrientationType.DEFAULT,
                    readerTheme = 1,
                    pageLayout = PageLayout.SINGLE_PAGE,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ReaderSettingsSheetWebtoonPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        ReaderSettingsSheet(
            uiState =
                ReaderSettingsUiState(
                    isWebtoon = true,
                    readingMode = ReadingModeType.WEBTOON,
                    orientation = OrientationType.DEFAULT,
                    readerTheme = 1,
                    webtoonSidePadding = 10,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ReaderSettingsSheetFilterPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        ReaderSettingsSheet(
            uiState =
                ReaderSettingsUiState(
                    selectedTab = 2,
                    grayscale = true,
                    customBrightness = true,
                    customBrightnessValue = 20,
                    colorFilter = true,
                    colorFilterMode = 1,
                    colorFilterValue = 0x80FF0000.toInt(),
                ),
            onAction = {},
        )
    }
}
