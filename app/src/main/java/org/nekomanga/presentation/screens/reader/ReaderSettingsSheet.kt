package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import org.nekomanga.R
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

@Composable
fun ReaderSettingsSheet(
    onDismiss: () -> Unit,
    viewModel: eu.kanade.tachiyomi.ui.reader.ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val readerPreferences: ReaderPreferences = remember { Injekt.get() }
    val themeColorState = defaultThemeColorState()
    val context = LocalContext.current
    val activity = remember(context) { context as? ReaderActivity }

    // Preferences observation
    val readerTheme by readerPreferences.readerTheme().collectAsState()
    val showPageNumber by readerPreferences.showPageNumber().collectAsState()
    val keepScreenOn by readerPreferences.keepScreenOn().collectAsState()
    val alwaysShowChapterTransition by readerPreferences.alwaysShowChapterTransition().collectAsState()

    // Pager preferences
    val imageScaleType by readerPreferences.imageScaleType().collectAsState()
    val doublePageGap by readerPreferences.doublePageGap().collectAsState()
    val navigateToPan by readerPreferences.navigateToPan().collectAsState()
    val landscapeZoom by readerPreferences.landscapeZoom().collectAsState()
    val zoomStart by readerPreferences.zoomStart().collectAsState()
    val cropBorders by readerPreferences.cropBorders().collectAsState()
    val pageTransitions by readerPreferences.animatedPageTransitions().collectAsState()
    val pagerNav by readerPreferences.navigationModePager().collectAsState()
    val pagerInvert by readerPreferences.pagerNavInverted().collectAsState()
    val pagerCutoutBehavior by readerPreferences.pagerCutoutBehavior().collectAsState()
    val pageLayoutPref by readerPreferences.pageLayout().collectAsState()
    val invertDoublePages by readerPreferences.invertDoublePages().collectAsState()
    val doublePageRotate by readerPreferences.doublePageRotate().collectAsState()
    val doublePageRotateReverse by readerPreferences.doublePageRotateReverse().collectAsState()

    // Webtoon preferences
    val cropBordersWebtoon by readerPreferences.cropBordersWebtoon().collectAsState()
    val webtoonSidePadding by readerPreferences.webtoonSidePadding().collectAsState()
    val webtoonEnableZoomOut by readerPreferences.webtoonEnableZoomOut().collectAsState()
    val webtoonNav by readerPreferences.navigationModeWebtoon().collectAsState()
    val webtoonInvert by readerPreferences.webtoonNavInverted().collectAsState()
    val webtoonPageLayout by readerPreferences.webtoonPageLayout().collectAsState()
    val webtoonInvertDoublePages by readerPreferences.webtoonInvertDoublePages().collectAsState()
    val webtoonPageTransitions by readerPreferences.animatedPageTransitionsWebtoon().collectAsState()
    val splitTallImages by readerPreferences.splitTallImagesReader().collectAsState()

    // Color filter & brightness & grayscale
    val grayscale by readerPreferences.grayscale().collectAsState()
    val invertedColors by readerPreferences.invertedColors().collectAsState()
    val colorFilter by readerPreferences.colorFilter().collectAsState()
    val colorFilterMode by readerPreferences.colorFilterMode().collectAsState()
    val colorFilterValue by readerPreferences.colorFilterValue().collectAsState()
    val customBrightness by readerPreferences.customBrightness().collectAsState()
    val customBrightnessValue by readerPreferences.customBrightnessValue().collectAsState()

    // Local state for color components
    val alpha = (colorFilterValue shr 24) and 0xFF
    val red = (colorFilterValue shr 16) and 0xFF
    val green = (colorFilterValue shr 8) and 0xFF
    val blue = colorFilterValue and 0xFF

    // Check if the current reading mode is Webtoon type
    val currentReadingMode = viewModel.getMangaReadingMode()
    val isWebtoon = ReadingModeType.isWebtoonType(currentReadingMode)

    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = 0.85f,
        bottomPaddingAroundContent = 24.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.reader_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- Section 1: General Settings ---
            SettingsSectionHeader(title = stringResource(R.string.general))

            // Reading Mode Spinner
            val readingModeOptions = stringArrayResource(id = R.array.viewers_selector).toList()
            val currentModeIndex = ReadingModeType.fromPreference(currentReadingMode).prefValue
            ReaderDropdownSelector(
                label = stringResource(R.string.reading_mode),
                options = readingModeOptions,
                selectedIndex = currentModeIndex,
                onSelected = { index ->
                    val readingModeType = ReadingModeType.fromSpinner(index)
                    viewModel.setMangaReadingMode(readingModeType.flagValue)
                }
            )

            // Rotation/Orientation Spinner
            val rotationOptions = stringArrayResource(id = R.array.rotation_type).toList()
            val currentRotationIndex = viewModel.manga?.orientationType?.let {
                OrientationType.fromPreference(it).prefValue
            } ?: 0
            ReaderDropdownSelector(
                label = stringResource(R.string.rotation),
                options = rotationOptions,
                selectedIndex = currentRotationIndex,
                onSelected = { index ->
                    val rotationType = OrientationType.fromSpinner(index)
                    viewModel.setMangaOrientationType(rotationType.flagValue)
                }
            )

            // Background Theme
            val themeOptions = stringArrayResource(id = R.array.reader_themes).toList()
            ReaderDropdownSelector(
                label = stringResource(R.string.background_color),
                options = themeOptions,
                selectedIndex = readerTheme,
                onSelected = { index ->
                    readerPreferences.readerTheme().set(index)
                }
            )

            // Checkbox settings
            ReaderSwitchSetting(
                label = stringResource(R.string.show_page_number),
                checked = showPageNumber,
                onCheckedChange = { readerPreferences.showPageNumber().set(it) }
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.keep_screen_on),
                checked = keepScreenOn,
                onCheckedChange = { readerPreferences.keepScreenOn().set(it) }
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.always_show_chapter_transition),
                checked = alwaysShowChapterTransition,
                onCheckedChange = { readerPreferences.alwaysShowChapterTransition().set(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            // --- Section 2: Paged / Webtoon Specific Settings ---
            if (isWebtoon) {
                SettingsSectionHeader(title = stringResource(R.string.webtoon))

                val hasMargins = (activity?.viewer as? eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer)?.hasMargins ?: false
                ReaderSwitchSetting(
                    label = stringResource(R.string.crop_borders),
                    checked = if (hasMargins) cropBorders else cropBordersWebtoon,
                    onCheckedChange = {
                        if (hasMargins) {
                            readerPreferences.cropBorders().set(it)
                        } else {
                            readerPreferences.cropBordersWebtoon().set(it)
                        }
                    }
                )

                // Side padding
                val sidePaddingOptions = stringArrayResource(id = R.array.webtoon_side_padding).toList()
                val sidePaddingValues = stringArrayResource(id = R.array.webtoon_side_padding_values).map { it.toInt() }
                val selectedSidePaddingIndex = maxOf(0, sidePaddingValues.indexOf(webtoonSidePadding))
                ReaderDropdownSelector(
                    label = stringResource(R.string.pref_webtoon_side_padding),
                    options = sidePaddingOptions,
                    selectedIndex = selectedSidePaddingIndex,
                    onSelected = { index ->
                        readerPreferences.webtoonSidePadding().set(sidePaddingValues[index])
                    }
                )

                ReaderSwitchSetting(
                    label = stringResource(R.string.enable_zoom_out),
                    checked = webtoonEnableZoomOut,
                    onCheckedChange = { readerPreferences.webtoonEnableZoomOut().set(it) }
                )

                val webtoonNavOptions = stringArrayResource(id = R.array.reader_nav).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.tap_zones),
                    options = webtoonNavOptions,
                    selectedIndex = webtoonNav,
                    onSelected = { readerPreferences.navigationModeWebtoon().set(it) }
                )

                val invertTappingOptions = stringArrayResource(id = R.array.invert_tapping_mode).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.invert_tapping),
                    options = invertTappingOptions,
                    selectedIndex = webtoonInvert.ordinal,
                    onSelected = { index ->
                        readerPreferences.webtoonNavInverted().set(ViewerNavigation.TappingInvertMode.values()[index])
                    }
                )

                val webtoonLayoutOptions = stringArrayResource(id = R.array.webtoon_page_layouts).toList()
                val selectedWebtoonLayoutIndex = if (webtoonPageLayout == PageLayout.SPLIT_PAGES.webtoonValue) 1 else 0
                ReaderDropdownSelector(
                    label = stringResource(R.string.page_layout),
                    options = webtoonLayoutOptions,
                    selectedIndex = selectedWebtoonLayoutIndex,
                    onSelected = { index ->
                        val layoutValue = if (index == 1) PageLayout.SPLIT_PAGES.webtoonValue else PageLayout.SINGLE_PAGE.webtoonValue
                        readerPreferences.webtoonPageLayout().set(layoutValue)
                    }
                )

                if (webtoonPageLayout != PageLayout.SINGLE_PAGE.webtoonValue) {
                    ReaderSwitchSetting(
                        label = stringResource(R.string.invert_double_pages),
                        checked = webtoonInvertDoublePages,
                        onCheckedChange = { readerPreferences.webtoonInvertDoublePages().set(it) }
                    )
                }

                ReaderSwitchSetting(
                    label = stringResource(R.string.animate_page_transitions_webtoon),
                    checked = webtoonPageTransitions,
                    onCheckedChange = { readerPreferences.animatedPageTransitionsWebtoon().set(it) }
                )

                ReaderSwitchSetting(
                    label = stringResource(R.string.split_tall_images_reader),
                    checked = splitTallImages,
                    onCheckedChange = { readerPreferences.splitTallImagesReader().set(it) }
                )
            } else {
                SettingsSectionHeader(title = stringResource(R.string.paged))

                val scaleOptions = stringArrayResource(id = R.array.image_scale_type).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.scale_type),
                    options = scaleOptions,
                    selectedIndex = imageScaleType - 1,
                    onSelected = { index ->
                        readerPreferences.imageScaleType().set(index + 1)
                    }
                )

                val zoomStartOptions = stringArrayResource(id = R.array.zoom_start).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.zoom_start_position),
                    options = zoomStartOptions,
                    selectedIndex = zoomStart - 1,
                    onSelected = { index ->
                        readerPreferences.zoomStart().set(index + 1)
                    }
                )

                ReaderSwitchSetting(
                    label = stringResource(R.string.crop_borders),
                    checked = cropBorders,
                    onCheckedChange = { readerPreferences.cropBorders().set(it) }
                )

                ReaderSwitchSetting(
                    label = stringResource(R.string.animate_page_transitions),
                    checked = pageTransitions,
                    onCheckedChange = { readerPreferences.animatedPageTransitions().set(it) }
                )

                val pagerNavOptions = stringArrayResource(id = R.array.reader_nav).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.tap_zones),
                    options = pagerNavOptions,
                    selectedIndex = pagerNav,
                    onSelected = { readerPreferences.navigationModePager().set(it) }
                )

                val invertTappingOptions = stringArrayResource(id = R.array.invert_tapping_mode).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.invert_tapping),
                    options = invertTappingOptions,
                    selectedIndex = pagerInvert.ordinal,
                    onSelected = { index ->
                        readerPreferences.pagerNavInverted().set(ViewerNavigation.TappingInvertMode.values()[index])
                    }
                )

                val pagedLayoutOptions = stringArrayResource(id = R.array.page_layouts).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.page_layout),
                    options = pagedLayoutOptions,
                    selectedIndex = pageLayoutPref,
                    onSelected = { readerPreferences.pageLayout().set(it) }
                )

                if (pageLayoutPref != PageLayout.SINGLE_PAGE.value) {
                    ReaderSwitchSetting(
                        label = stringResource(R.string.invert_double_pages),
                        checked = invertDoublePages,
                        onCheckedChange = { readerPreferences.invertDoublePages().set(it) }
                    )

                    val gapOptions = stringArrayResource(id = R.array.double_page_gap).toList()
                    val selectedGapIndex = maxOf(0, gapOptions.indexOf(doublePageGap.toString()))
                    ReaderDropdownSelector(
                        label = stringResource(R.string.double_page_gap),
                        options = gapOptions,
                        selectedIndex = selectedGapIndex,
                        onSelected = { index ->
                            readerPreferences.doublePageGap().set(gapOptions[index].toInt())
                        }
                    )
                }

                if (imageScaleType - 1 == 3) { // Center Inside
                    ReaderSwitchSetting(
                        label = stringResource(R.string.zoom_double_page_spreads),
                        checked = landscapeZoom,
                        onCheckedChange = { readerPreferences.landscapeZoom().set(it) }
                    )
                }

                ReaderSwitchSetting(
                    label = stringResource(R.string.navigate_pan),
                    checked = navigateToPan,
                    onCheckedChange = { readerPreferences.navigateToPan().set(it) }
                )

                ReaderSwitchSetting(
                    label = stringResource(R.string.double_page_rotate),
                    checked = doublePageRotate,
                    onCheckedChange = { readerPreferences.doublePageRotate().set(it) }
                )

                if (doublePageRotate) {
                    ReaderSwitchSetting(
                        label = stringResource(R.string.double_page_rotate_reverse),
                        checked = doublePageRotateReverse,
                        onCheckedChange = { readerPreferences.doublePageRotateReverse().set(it) }
                    )
                }

                // Extend past cutout
                val isFullFit = imageScaleType - 1 in listOf(0, 1, 5) // smart fit / stretch / fit screen
                val hasCutout = activity?.window?.decorView?.let { decorView ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        decorView.rootWindowInsets?.displayCutout?.safeInsetTop != null ||
                                decorView.rootWindowInsets?.displayCutout?.safeInsetBottom != null
                    } else false
                } ?: false

                if (isFullFit && hasCutout && keepScreenOn) {
                    val cutoutOptions = stringArrayResource(id = R.array.cutout_behavior).toList()
                    ReaderDropdownSelector(
                        label = stringResource(R.string.cutout_area_behavior),
                        options = cutoutOptions,
                        selectedIndex = pagerCutoutBehavior,
                        onSelected = { readerPreferences.pagerCutoutBehavior().set(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            // --- Section 3: Colors & Filters ---
            SettingsSectionHeader(title = stringResource(R.string.filter))

            ReaderSwitchSetting(
                label = stringResource(R.string.grayscale),
                checked = grayscale,
                onCheckedChange = { readerPreferences.grayscale().set(it) }
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.pref_inverted_colors),
                checked = invertedColors,
                onCheckedChange = { readerPreferences.invertedColors().set(it) }
            )

            ReaderSwitchSetting(
                label = stringResource(R.string.use_custom_brightness),
                checked = customBrightness,
                onCheckedChange = { readerPreferences.customBrightness().set(it) }
            )

            if (customBrightness) {
                ReaderSliderSetting(
                    label = stringResource(R.string.use_custom_brightness),
                    value = customBrightnessValue.toFloat(),
                    valueRange = -75f..100f,
                    valueFormatter = { it.roundToInt().toString() },
                    onValueChange = { readerPreferences.customBrightnessValue().set(it.roundToInt()) }
                )
            }

            ReaderSwitchSetting(
                label = stringResource(R.string.use_custom_color_filter),
                checked = colorFilter,
                onCheckedChange = { readerPreferences.colorFilter().set(it) }
            )

            if (colorFilter) {
                val blendModes = stringArrayResource(id = R.array.color_filter_modes).toList()
                ReaderDropdownSelector(
                    label = stringResource(R.string.color_filter_blend_mode),
                    options = blendModes,
                    selectedIndex = colorFilterMode,
                    onSelected = { readerPreferences.colorFilterMode().set(it) }
                )

                // Alpha Slider
                ReaderSliderSetting(
                    label = stringResource(R.string.alpha_initial),
                    value = alpha.toFloat(),
                    valueRange = 0f..255f,
                    valueFormatter = { it.roundToInt().toString() },
                    onValueChange = { value ->
                        updateColorComponent(value.roundToInt(), ALPHA_MASK, 24, readerPreferences)
                    }
                )

                // Red Slider
                ReaderSliderSetting(
                    label = stringResource(R.string.red_initial),
                    value = red.toFloat(),
                    valueRange = 0f..255f,
                    valueFormatter = { it.roundToInt().toString() },
                    onValueChange = { value ->
                        updateColorComponent(value.roundToInt(), RED_MASK, 16, readerPreferences)
                    }
                )

                // Green Slider
                ReaderSliderSetting(
                    label = stringResource(R.string.green_initial),
                    value = green.toFloat(),
                    valueRange = 0f..255f,
                    valueFormatter = { it.roundToInt().toString() },
                    onValueChange = { value ->
                        updateColorComponent(value.roundToInt(), GREEN_MASK, 8, readerPreferences)
                    }
                )

                // Blue Slider
                ReaderSliderSetting(
                    label = stringResource(R.string.blue_initial),
                    value = blue.toFloat(),
                    valueRange = 0f..255f,
                    valueFormatter = { it.roundToInt().toString() },
                    onValueChange = { value ->
                        updateColorComponent(value.roundToInt(), BLUE_MASK, 0, readerPreferences)
                    }
                )
            }
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
        modifier = modifier.padding(vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderDropdownSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        OutlinedTextField(
            value = options.getOrNull(selectedIndex) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
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
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
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
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                thumbColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

private fun updateColorComponent(
    value: Int,
    mask: Long,
    bitShift: Int,
    readerPreferences: ReaderPreferences,
) {
    val currentColor = readerPreferences.colorFilterValue().get()
    val updatedColor = (value shl bitShift) or (currentColor and mask.inv().toInt())
    readerPreferences.colorFilterValue().set(updatedColor)
}

private const val ALPHA_MASK: Long = 0xFF000000
private const val RED_MASK: Long = 0x00FF0000
private const val GREEN_MASK: Long = 0x0000FF00
private const val BLUE_MASK: Long = 0x000000FF
