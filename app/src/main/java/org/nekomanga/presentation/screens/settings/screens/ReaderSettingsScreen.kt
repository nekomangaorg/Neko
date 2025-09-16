package org.nekomanga.presentation.screens.settings.screens

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import org.nekomanga.R
import org.nekomanga.core.preferences.PreferenceValues
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class ReaderSettingsScreen(
    val readerPreferences: ReaderPreferences,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.reader_settings

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        return persistentListOf(
            generalGroup(),
            displayGroup(),
            einkGroup(),
            readingGroup(),
            pagedGroup(),
            webtoonGroup(),
            navigationGroup(),
            actionsGroup(),
        )
    }

    @Composable
    private fun generalGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.general),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.defaultReadingMode(),
                        title = stringResource(R.string.default_reading_mode),
                        entries =
                            ReadingModeType.entries
                                .drop(1)
                                .associate { type ->
                                    type.prefValue to stringResource(type.stringRes)
                                }
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.doubleTapAnimSpeed(),
                        title = stringResource(R.string.double_tap_anim_speed),
                        entries =
                            persistentMapOf(
                                1 to
                                    stringResource(
                                        R.string.no_animation
                                    ), // using a value of 0 breaks the image viewer, so
                                250 to stringResource(R.string.normal),
                                500 to stringResource(R.string.fast),
                            ),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.trueColor(),
                        title = stringResource(R.string.true_32bit_color),
                        subtitle = stringResource(R.string.reduces_banding_impacts_performance),
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.preloadPageAmount(),
                        title = stringResource(R.string.page_preload_amount),
                        subtitle = stringResource(R.string.amount_of_pages_to_preload),
                        entries =
                            listOf(4, 6, 8, 10, 12, 14, 16, 20)
                                .associateWith {
                                    pluralStringResource(R.plurals.pages_plural, it, it)
                                }
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.MultiSelectListPreference(
                        pref = readerPreferences.readerBottomButtons(),
                        title = stringResource(R.string.display_buttons_bottom_reader),
                        entries =
                            ReaderBottomButton.entries
                                .associate { it.value to stringResource(it.stringRes) }
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(R.string.certain_buttons_can_be_found)
                    ),
                ),
        )
    }

    @Composable
    private fun displayGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.display),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.defaultOrientationType(),
                        title = stringResource(R.string.default_orientation),
                        entries =
                            OrientationType.entries
                                .drop(1)
                                .associate { type ->
                                    type.flagValue to stringResource(type.stringRes)
                                }
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.readerTheme(),
                        title = stringResource(R.string.background_color),
                        entries =
                            mapOf(
                                    0 to stringResource(R.string.white),
                                    1 to stringResource(R.string.black),
                                    2 to stringResource(R.string.smart_based_on_page),
                                    3 to stringResource(R.string.smart_based_on_page_and_theme),
                                    4 to
                                        stringResource(
                                            R.string.smart_based_on_page_and_theme_use_black
                                        ),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.fullscreen(),
                        title = stringResource(R.string.fullscreen),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.keepScreenOn(),
                        title = stringResource(R.string.keep_screen_on),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.showPageNumber(),
                        title = stringResource(R.string.show_page_number),
                    ),
                ),
        )
    }

    @Composable
    private fun einkGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.eink),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.grayscale(),
                        title = stringResource(R.string.grayscale),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.colorEInk16bit(),
                        title = stringResource(R.string.eink_16bit),
                        subtitle = stringResource(R.string.eink_16bit_description),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.colorEInkDither(),
                        title = stringResource(R.string.eink_dither),
                        subtitle = stringResource(R.string.eink_dither_description),
                        enabled = readerPreferences.colorEInk16bit().collectAsState().value,
                    ),
                ),
        )
    }

    @Composable
    private fun readingGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.reading),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.skipRead(),
                        title = stringResource(R.string.skip_read_chapters),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.skipFiltered(),
                        title = stringResource(R.string.skip_filtered_chapters),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.skipDuplicates(),
                        title = stringResource(R.string.skip_duplicate_chapters),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.alwaysShowChapterTransition(),
                        title = stringResource(R.string.always_show_chapter_transition),
                        subtitle = stringResource(R.string.if_disabled_transition_will_skip),
                    ),
                ),
        )
    }

    @Composable
    private fun pagedGroup(): Preference.PreferenceGroup {
        val displayCutoutInsets = WindowInsets.displayCutout

        val hasDisplayCutout =
            displayCutoutInsets.getLeft(density = LocalDensity.current, LayoutDirection.Rtl) > 0 ||
                displayCutoutInsets.getTop(density = LocalDensity.current) > 0 ||
                displayCutoutInsets.getRight(density = LocalDensity.current, LayoutDirection.Rtl) >
                    0 ||
                displayCutoutInsets.getBottom(density = LocalDensity.current) > 0

        val imageScaleType by readerPreferences.imageScaleType().collectAsState()
        val pageLayoutType by readerPreferences.pageLayout().collectAsState()

        val showZoomDoublePageSpread by
            remember(imageScaleType) { mutableStateOf(imageScaleType == 1) }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.paged),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.navigationModePager(),
                        title = stringResource(R.string.tap_zones),
                        entries =
                            stringArrayResource(R.array.reader_nav)
                                .mapIndexed { index, string -> index to string }
                                .toMap()
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.pagerNavInverted(),
                        title = stringResource(R.string.invert_tapping),
                        entries =
                            mapOf(
                                    ViewerNavigation.TappingInvertMode.NONE to
                                        stringResource(R.string.none),
                                    ViewerNavigation.TappingInvertMode.HORIZONTAL to
                                        stringResource(R.string.horizontally),
                                    ViewerNavigation.TappingInvertMode.VERTICAL to
                                        stringResource(R.string.vertically),
                                    ViewerNavigation.TappingInvertMode.BOTH to
                                        stringResource(R.string.both_axes),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.imageScaleType(),
                        title = stringResource(R.string.scale_type),
                        entries =
                            mapOf(
                                    1 to stringResource(R.string.fit_screen),
                                    2 to stringResource(R.string.stretch),
                                    3 to stringResource(R.string.fit_width),
                                    4 to stringResource(R.string.fit_height),
                                    5 to stringResource(R.string.original_size),
                                    6 to stringResource(R.string.smart_fit),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.pagerCutoutBehavior(),
                        title = stringResource(R.string.cutout_area_behavior),
                        subtitle = stringResource(R.string.cutout_behavior_only_applies),
                        entries =
                            mapOf(
                                    0 to stringResource(R.string.pad_cutout_areas),
                                    1 to stringResource(R.string.start_past_cutout),
                                    2 to stringResource(R.string.ignore_cutout_areas),
                                )
                                .toPersistentMap(),
                        enabled = hasDisplayCutout,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.landscapeZoom(),
                        title = stringResource(R.string.zoom_double_page_spreads),
                        enabled = showZoomDoublePageSpread,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.zoomStart(),
                        title = stringResource(R.string.zoom_start_position),
                        entries =
                            mapOf(
                                    1 to stringResource(R.string.automatic),
                                    2 to stringResource(R.string.left),
                                    3 to stringResource(R.string.right),
                                    4 to stringResource(R.string.center),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.cropBorders(),
                        title = stringResource(R.string.crop_borders),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.animatedPageTransitions(),
                        title = stringResource(R.string.animate_page_transitions),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.navigateToPan(),
                        title = stringResource(R.string.navigate_pan),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.pageLayout(),
                        title = stringResource(R.string.page_layout),
                        entries =
                            PageLayout.entries
                                .associate { layout ->
                                    layout.value to stringResource(layout.fullStringRes)
                                }
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(R.string.automatic_can_still_switch)
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.automaticSplitsPage(),
                        title = stringResource(R.string.split_double_pages_portrait),
                        enabled = pageLayoutType == PageLayout.AUTOMATIC.value,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.invertDoublePages(),
                        title = stringResource(R.string.invert_double_pages),
                        enabled = pageLayoutType != PageLayout.SINGLE_PAGE.value,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.doublePageRotate(),
                        title = stringResource(R.string.double_page_rotate),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.doublePageRotateReverse(),
                        title = stringResource(R.string.double_page_rotate_reverse),
                        enabled = readerPreferences.doublePageRotate().collectAsState().value,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.doublePageGap(),
                        title = stringResource(R.string.double_page_gap),
                        entries =
                            mapOf(
                                    0 to stringResource(R.string.double_page_gap_0),
                                    10 to stringResource(R.string.double_page_gap_10),
                                    20 to stringResource(R.string.double_page_gap_20),
                                    30 to stringResource(R.string.double_page_gap_30),
                                    40 to stringResource(R.string.double_page_gap_40),
                                    50 to stringResource(R.string.double_page_gap_50),
                                    60 to stringResource(R.string.double_page_gap_60),
                                    70 to stringResource(R.string.double_page_gap_70),
                                )
                                .toPersistentMap(),
                    ),
                ),
        )
    }

    @Composable
    private fun webtoonGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.webtoon),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.navigationModeWebtoon(),
                        title = stringResource(R.string.tap_zones),
                        entries =
                            stringArrayResource(R.array.reader_nav)
                                .mapIndexed { index, string -> index to string }
                                .toMap()
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.webtoonNavInverted(),
                        title = stringResource(R.string.invert_tapping),
                        entries =
                            mapOf(
                                    ViewerNavigation.TappingInvertMode.NONE to
                                        stringResource(R.string.none),
                                    ViewerNavigation.TappingInvertMode.HORIZONTAL to
                                        stringResource(R.string.horizontally),
                                    ViewerNavigation.TappingInvertMode.VERTICAL to
                                        stringResource(R.string.vertically),
                                    ViewerNavigation.TappingInvertMode.BOTH to
                                        stringResource(R.string.both_axes),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.webtoonReaderHideThreshold(),
                        title = stringResource(R.string.pref_hide_threshold),
                        entries =
                            PreferenceValues.ReaderHideThreshold.entries
                                .associate { it to stringResource(it.titleResId) }
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.cropBordersWebtoon(),
                        title = stringResource(R.string.crop_borders),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.webtoonSidePadding(),
                        title = stringResource(R.string.pref_webtoon_side_padding),
                        entries =
                            mapOf(
                                    0 to stringResource(R.string.webtoon_side_padding_0),
                                    5 to stringResource(R.string.webtoon_side_padding_5),
                                    10 to stringResource(R.string.webtoon_side_padding_10),
                                    15 to stringResource(R.string.webtoon_side_padding_15),
                                    20 to stringResource(R.string.webtoon_side_padding_20),
                                    25 to stringResource(R.string.webtoon_side_padding_25),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = readerPreferences.webtoonPageLayout(),
                        title = stringResource(R.string.page_layout),
                        entries =
                            mapOf(
                                    PageLayout.SINGLE_PAGE.webtoonValue to
                                        stringResource(PageLayout.SINGLE_PAGE.fullStringRes),
                                    PageLayout.SPLIT_PAGES.webtoonValue to
                                        stringResource(PageLayout.SPLIT_PAGES.fullStringRes),
                                )
                                .toPersistentMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.webtoonInvertDoublePages(),
                        title = stringResource(R.string.invert_double_pages),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.animatedPageTransitionsWebtoon(),
                        title = stringResource(R.string.animate_page_transitions_webtoon),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.webtoonEnableZoomOut(),
                        title = stringResource(R.string.enable_zoom_out),
                    ),
                ),
        )
    }

    @Composable
    private fun navigationGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.navigation),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.readWithVolumeKeys(),
                        title = stringResource(R.string.volume_keys),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.readWithVolumeKeysInverted(),
                        title = stringResource(R.string.invert_volume_keys),
                        enabled = readerPreferences.readWithVolumeKeys().collectAsState().value,
                    ),
                ),
        )
    }

    @Composable
    private fun actionsGroup(): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.actions),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.readWithLongTap(),
                        title = stringResource(R.string.show_on_long_press),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = readerPreferences.folderPerManga(),
                        title = stringResource(R.string.save_pages_separately),
                        subtitle = stringResource(R.string.create_folders_by_manga_title),
                    ),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf(
                SearchTerm(
                    title = stringResource(R.string.default_reading_mode),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.double_tap_anim_speed),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.true_32bit_color),
                    subtitle = stringResource(R.string.reduces_banding_impacts_performance),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.page_preload_amount),
                    subtitle = stringResource(R.string.amount_of_pages_to_preload),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.display_buttons_bottom_reader),
                    group = stringResource(R.string.general),
                ),
                SearchTerm(
                    title = stringResource(R.string.default_orientation),
                    group = stringResource(R.string.display),
                ),
                SearchTerm(
                    title = stringResource(R.string.background_color),
                    group = stringResource(R.string.display),
                ),
                SearchTerm(
                    title = stringResource(R.string.fullscreen),
                    group = stringResource(R.string.display),
                ),
                SearchTerm(
                    title = stringResource(R.string.keep_screen_on),
                    group = stringResource(R.string.display),
                ),
                SearchTerm(
                    title = stringResource(R.string.show_page_number),
                    group = stringResource(R.string.display),
                ),
                SearchTerm(
                    title = stringResource(R.string.grayscale),
                    group = stringResource(R.string.eink),
                ),
                SearchTerm(
                    title = stringResource(R.string.eink_16bit),
                    subtitle = stringResource(R.string.eink_16bit_description),
                    group = stringResource(R.string.eink),
                ),
                SearchTerm(
                    title = stringResource(R.string.eink_dither),
                    subtitle = stringResource(R.string.eink_dither_description),
                    group = stringResource(R.string.eink),
                ),
                SearchTerm(
                    title = stringResource(R.string.skip_read_chapters),
                    group = stringResource(R.string.reading),
                ),
                SearchTerm(
                    title = stringResource(R.string.skip_filtered_chapters),
                    group = stringResource(R.string.reading),
                ),
                SearchTerm(
                    title = stringResource(R.string.skip_duplicate_chapters),
                    group = stringResource(R.string.reading),
                ),
                SearchTerm(
                    title = stringResource(R.string.always_show_chapter_transition),
                    subtitle = stringResource(R.string.if_disabled_transition_will_skip),
                    group = stringResource(R.string.reading),
                ),
                SearchTerm(
                    title = stringResource(R.string.tap_zones),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.invert_tapping),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.scale_type),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.cutout_area_behavior),
                    subtitle = stringResource(R.string.cutout_behavior_only_applies),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.zoom_double_page_spreads),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.zoom_start_position),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.crop_borders),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.animate_page_transitions),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.navigate_pan),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.page_layout),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.split_double_pages_portrait),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.invert_double_pages),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.double_page_rotate),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.double_page_rotate_reverse),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.double_page_gap),
                    group = stringResource(R.string.paged),
                ),
                SearchTerm(
                    title = stringResource(R.string.tap_zones),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.invert_tapping),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.pref_hide_threshold),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.crop_borders),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.pref_webtoon_side_padding),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.page_layout),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.invert_double_pages),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.animate_page_transitions_webtoon),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.enable_zoom_out),
                    group = stringResource(R.string.webtoon),
                ),
                SearchTerm(
                    title = stringResource(R.string.volume_keys),
                    group = stringResource(R.string.navigation),
                ),
                SearchTerm(
                    title = stringResource(R.string.invert_volume_keys),
                    group = stringResource(R.string.navigation),
                ),
                SearchTerm(
                    title = stringResource(R.string.show_on_long_press),
                    group = stringResource(R.string.actions),
                ),
                SearchTerm(
                    title = stringResource(R.string.save_pages_separately),
                    subtitle = stringResource(R.string.create_folders_by_manga_title),
                    group = stringResource(R.string.actions),
                ),
                SearchTerm(
                    title = stringResource(R.string.save_pages_separately),
                    subtitle = stringResource(R.string.create_folders_by_manga_title),
                ),
            )
        }
    }
}
