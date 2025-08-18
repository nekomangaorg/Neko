package eu.kanade.tachiyomi.ui.setting

import android.os.Build
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.util.view.activityBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import org.nekomanga.core.preferences.PreferenceValues

class SettingsReaderController : AbstractSettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.reader

            preferenceCategory {
                titleRes = R.string.general
                intListPreference(activity) {
                    key = readerPreferences.defaultReadingMode().key()
                    titleRes = R.string.default_reading_mode
                    entriesRes =
                        ReadingModeType.values()
                            .drop(1)
                            .dropLast(1)
                            .map { value -> value.stringRes }
                            .toTypedArray()
                    entryValues = ReadingModeType.values().drop(1).map { value -> value.flagValue }
                    defaultValue = 2
                }
                intListPreference(activity) {
                    key = readerPreferences.doubleTapAnimSpeed().key()
                    titleRes = R.string.double_tap_anim_speed
                    entries =
                        listOf(
                            context.getString(R.string.no_animation),
                            context.getString(R.string.fast),
                            context.getString(R.string.normal),
                        )
                    entryValues =
                        listOf(1, 250, 500) // using a value of 0 breaks the image viewer, so
                    // min is 1
                    defaultValue = 500
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    switchPreference {
                        key = readerPreferences.trueColor().key()
                        titleRes = R.string.true_32bit_color
                        summaryRes = R.string.reduces_banding_impacts_performance
                        defaultValue = false
                    }
                }
                intListPreference(activity) {
                    key = readerPreferences.preloadPageAmount().key()
                    titleRes = R.string.page_preload_amount
                    entryValues = listOf(4, 6, 8, 10, 12, 14, 16, 20)
                    entries =
                        entryValues.map {
                            context.resources.getQuantityString(R.plurals.pages_plural, it, it)
                        }
                    defaultValue = 6
                    summaryRes = R.string.amount_of_pages_to_preload
                }
                multiSelectListPreferenceMat(activity) {
                    key = readerPreferences.readerBottomButtons().key()
                    titleRes = R.string.display_buttons_bottom_reader
                    val enumConstants = ReaderBottomButton.values()
                    entriesRes = ReaderBottomButton.values().map { it.stringRes }.toTypedArray()
                    entryValues = enumConstants.map { it.value }
                    allSelectionRes = R.string.display_options
                    allIsAlwaysSelected = true
                    showAllLast = true
                    val defaults = ReaderBottomButton.BUTTONS_DEFAULTS.toMutableList()
                    if (context.isTablet()) {
                        defaults.add(ReaderBottomButton.ShiftDoublePage.value)
                    }
                    defaultValue = defaults
                }
                infoPreference(R.string.certain_buttons_can_be_found)
            }

            preferenceCategory {
                titleRes = R.string.display

                intListPreference(activity) {
                    key = readerPreferences.defaultOrientationType().key()
                    titleRes = R.string.default_orientation
                    val enumConstants = OrientationType.values().drop(1)
                    entriesRes = enumConstants.map { it.stringRes }.toTypedArray()
                    entryValues = OrientationType.values().drop(1).map { value -> value.flagValue }
                    defaultValue = OrientationType.FREE.flagValue
                }
                intListPreference(activity) {
                    key = readerPreferences.readerTheme().key()
                    titleRes = R.string.background_color
                    entriesRes =
                        arrayOf(
                            R.string.white,
                            R.string.black,
                            R.string.smart_based_on_page,
                            R.string.smart_based_on_page_and_theme,
                            R.string.smart_based_on_page_and_theme_use_black,
                        )
                    entryRange = 0..4
                    defaultValue = 2
                }
                switchPreference {
                    key = readerPreferences.fullscreen().key()
                    titleRes = R.string.fullscreen
                    defaultValue = true
                }
                switchPreference {
                    key = readerPreferences.keepScreenOn().key()
                    titleRes = R.string.keep_screen_on
                    defaultValue = true
                }
                switchPreference {
                    key = readerPreferences.showPageNumber().key()
                    titleRes = R.string.show_page_number
                    defaultValue = true
                }
            }

            preferenceCategory {
                titleRes = R.string.reading

                switchPreference {
                    key = readerPreferences.skipRead().key()
                    titleRes = R.string.skip_read_chapters
                    defaultValue = false
                }
                switchPreference {
                    key = readerPreferences.skipFiltered().key()
                    titleRes = R.string.skip_filtered_chapters
                    defaultValue = true
                }

                switchPreference {
                    bindTo(readerPreferences.skipDuplicates())
                    titleRes = R.string.skip_duplicate_chapters
                }

                switchPreference {
                    key = readerPreferences.alwaysShowChapterTransition().key()
                    titleRes = R.string.always_show_chapter_transition
                    summaryRes = R.string.if_disabled_transition_will_skip
                    defaultValue = true
                }
            }

            preferenceCategory {
                titleRes = R.string.paged

                intListPreference(activity) {
                    key = readerPreferences.navigationModePager().key()
                    titleRes = R.string.tap_zones
                    entries =
                        context.resources
                            .getStringArray(R.array.reader_nav)
                            .also { values -> entryRange = 0..values.size }
                            .toList()
                    defaultValue = "0"
                }
                listPreference(activity) {
                    key = readerPreferences.pagerNavInverted().key()
                    titleRes = R.string.invert_tapping
                    entriesRes =
                        arrayOf(
                            R.string.none,
                            R.string.horizontally,
                            R.string.vertically,
                            R.string.both_axes,
                        )
                    entryValues =
                        listOf(
                            ViewerNavigation.TappingInvertMode.NONE.name,
                            ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                            ViewerNavigation.TappingInvertMode.VERTICAL.name,
                            ViewerNavigation.TappingInvertMode.BOTH.name,
                        )
                    defaultValue = ViewerNavigation.TappingInvertMode.NONE.name
                }

                intListPreference(activity) {
                    key = readerPreferences.imageScaleType().key()
                    titleRes = R.string.scale_type
                    entriesRes =
                        arrayOf(
                            R.string.fit_screen,
                            R.string.stretch,
                            R.string.fit_width,
                            R.string.fit_height,
                            R.string.original_size,
                            R.string.smart_fit,
                        )
                    entryRange = 1..6
                    defaultValue = 1
                }

                intListPreference(activity) {
                    key = readerPreferences.pagerCutoutBehavior().key()
                    titleRes = R.string.cutout_area_behavior
                    entriesRes =
                        arrayOf(
                            R.string.pad_cutout_areas,
                            R.string.start_past_cutout,
                            R.string.ignore_cutout_areas,
                        )
                    summaryRes = R.string.cutout_behavior_only_applies
                    entryRange = 0..2
                    defaultValue = 0
                    // Calling this once to show only on cutout
                    isVisible =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            activityBinding?.root?.rootWindowInsets?.displayCutout?.safeInsetTop !=
                                null ||
                                activityBinding
                                    ?.root
                                    ?.rootWindowInsets
                                    ?.displayCutout
                                    ?.safeInsetBottom != null
                        } else {
                            false
                        }
                    // Calling this a second time in case activity is recreated while on this page
                    // Keep the first so it shouldn't animate hiding the preference for phones
                    // without
                    // cutouts
                    activityBinding?.root?.post {
                        isVisible =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                activityBinding
                                    ?.root
                                    ?.rootWindowInsets
                                    ?.displayCutout
                                    ?.safeInsetTop != null ||
                                    activityBinding
                                        ?.root
                                        ?.rootWindowInsets
                                        ?.displayCutout
                                        ?.safeInsetBottom != null
                            } else {
                                false
                            }
                    }
                }
                switchPreference {
                    bindTo(readerPreferences.landscapeZoom())
                    titleRes = R.string.zoom_double_page_spreads
                    readerPreferences
                        .imageScaleType()
                        .changes()
                        .onEach { isVisible = it == 1 }
                        .launchIn(viewScope)
                }
                intListPreference(activity) {
                    key = readerPreferences.zoomStart().key()
                    titleRes = R.string.zoom_start_position
                    entriesRes =
                        arrayOf(R.string.automatic, R.string.left, R.string.right, R.string.center)
                    entryRange = 1..4
                    defaultValue = 1
                }
                switchPreference {
                    key = readerPreferences.cropBorders().key()
                    titleRes = R.string.crop_borders
                    defaultValue = false
                }
                switchPreference {
                    key = readerPreferences.animatedPageTransitions().key()
                    titleRes = R.string.animate_page_transitions
                    defaultValue = true
                }
                switchPreference {
                    bindTo(readerPreferences.navigateToPan())
                    titleRes = R.string.navigate_pan
                }
                intListPreference(activity) {
                    key = readerPreferences.pageLayout().key()
                    title =
                        context
                            .getString(R.string.page_layout)
                            .addBetaTag(context, R.attr.colorSecondary)
                    dialogTitleRes = R.string.page_layout
                    val enumConstants = PageLayout.values()
                    entriesRes = enumConstants.map { it.fullStringRes }.toTypedArray()
                    entryValues = enumConstants.map { it.value }
                    defaultValue = PageLayout.AUTOMATIC.value
                }
                infoPreference(R.string.automatic_can_still_switch).apply {
                    readerPreferences
                        .pageLayout()
                        .changes()
                        .onEach { isVisible = it == PageLayout.AUTOMATIC.value }
                        .launchIn(viewScope)
                }
                switchPreference {
                    key = readerPreferences.automaticSplitsPage().key()
                    titleRes = R.string.split_double_pages_portrait
                    defaultValue = false
                    readerPreferences
                        .pageLayout()
                        .changes()
                        .onEach { isVisible = it == PageLayout.AUTOMATIC.value }
                        .launchIn(viewScope)
                }
                switchPreference {
                    key = readerPreferences.invertDoublePages().key()
                    titleRes = R.string.invert_double_pages
                    defaultValue = false
                    readerPreferences
                        .pageLayout()
                        .changes()
                        .onEach { isVisible = it != PageLayout.SINGLE_PAGE.value }
                        .launchIn(viewScope)
                }

                switchPreference {
                    bindTo(readerPreferences.doublePageRotate())
                    titleRes = R.string.double_page_rotate
                }
                switchPreference {
                    readerPreferences
                        .doublePageRotate()
                        .changes()
                        .onEach { isVisible = it }
                        .launchIn(viewScope)
                    bindTo(readerPreferences.doublePageRotateReverse())
                    titleRes = R.string.double_page_rotate_reverse
                }

                intListPreference(activity) {
                    key = readerPreferences.doublePageGap().key()
                    titleRes = R.string.double_page_gap
                    entriesRes =
                        arrayOf(
                            R.string.double_page_gap_0,
                            R.string.double_page_gap_10,
                            R.string.double_page_gap_20,
                            R.string.double_page_gap_30,
                            R.string.double_page_gap_40,
                            R.string.double_page_gap_50,
                            R.string.double_page_gap_60,
                            R.string.double_page_gap_70,
                        )
                    entryValues = listOf(0, 10, 20, 30, 40, 50, 60, 70)
                    defaultValue = "0"
                }
            }
            preferenceCategory {
                titleRes = R.string.webtoon

                intListPreference(activity) {
                    key = readerPreferences.navigationModeWebtoon().key()
                    titleRes = R.string.tap_zones
                    entries =
                        context.resources
                            .getStringArray(R.array.reader_nav)
                            .also { values -> entryRange = 0..values.size }
                            .toList()
                    defaultValue = "0"
                }
                listPreference(activity) {
                    key = readerPreferences.webtoonNavInverted().key()
                    titleRes = R.string.invert_tapping
                    entriesRes =
                        arrayOf(
                            R.string.none,
                            R.string.horizontally,
                            R.string.vertically,
                            R.string.both_axes,
                        )
                    entryValues =
                        listOf(
                            ViewerNavigation.TappingInvertMode.NONE.name,
                            ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                            ViewerNavigation.TappingInvertMode.VERTICAL.name,
                            ViewerNavigation.TappingInvertMode.BOTH.name,
                        )
                    defaultValue = ViewerNavigation.TappingInvertMode.NONE.name
                }
                listPreference(activity) {
                    bindTo(readerPreferences.webtoonReaderHideThreshold())
                    titleRes = R.string.pref_hide_threshold
                    val enumValues = PreferenceValues.ReaderHideThreshold.values()
                    entriesRes = enumValues.map { it.titleResId }.toTypedArray()
                    entryValues = enumValues.map { it.name }
                }
                switchPreference {
                    key = readerPreferences.cropBordersWebtoon().key()
                    titleRes = R.string.crop_borders
                    defaultValue = false
                }

                intListPreference(activity) {
                    key = readerPreferences.webtoonSidePadding().key()
                    titleRes = R.string.pref_webtoon_side_padding
                    entriesRes =
                        arrayOf(
                            R.string.webtoon_side_padding_0,
                            R.string.webtoon_side_padding_5,
                            R.string.webtoon_side_padding_10,
                            R.string.webtoon_side_padding_15,
                            R.string.webtoon_side_padding_20,
                            R.string.webtoon_side_padding_25,
                        )
                    entryValues = listOf(0, 5, 10, 15, 20, 25)
                    defaultValue = "0"
                }

                intListPreference(activity) {
                    key = readerPreferences.webtoonPageLayout().key()
                    title = context.getString(R.string.page_layout)
                    dialogTitleRes = R.string.page_layout
                    val enumConstants = arrayOf(PageLayout.SINGLE_PAGE, PageLayout.SPLIT_PAGES)
                    entriesRes = enumConstants.map { it.fullStringRes }.toTypedArray()
                    entryValues = enumConstants.map { it.webtoonValue }
                    defaultValue = PageLayout.SINGLE_PAGE.value
                }

                switchPreference {
                    key = readerPreferences.webtoonInvertDoublePages().key()
                    titleRes = R.string.invert_double_pages
                    defaultValue = false
                }

                switchPreference {
                    key = readerPreferences.animatedPageTransitionsWebtoon().key()
                    titleRes = R.string.animate_page_transitions_webtoon
                    defaultValue = true
                }

                switchPreference {
                    key = readerPreferences.webtoonEnableZoomOut().key()
                    titleRes = R.string.enable_zoom_out
                    defaultValue = false
                }
            }

            preferenceCategory {
                titleRes = R.string.navigation

                switchPreference {
                    key = readerPreferences.readWithVolumeKeys().key()
                    titleRes = R.string.volume_keys
                    defaultValue = false
                }
                switchPreference {
                    key = readerPreferences.readWithVolumeKeysInverted().key()
                    titleRes = R.string.invert_volume_keys
                    defaultValue = false

                    readerPreferences
                        .readWithVolumeKeys()
                        .changes()
                        .onEach { isVisible = it }
                        .launchIn(viewScope)
                }
            }

            preferenceCategory {
                titleRes = R.string.actions

                switchPreference {
                    key = readerPreferences.readWithLongTap().key()
                    titleRes = R.string.show_on_long_press
                    defaultValue = true
                }
                switchPreference {
                    key = readerPreferences.folderPerManga().key()
                    titleRes = R.string.save_pages_separately
                    summaryRes = R.string.create_folders_by_manga_title
                    defaultValue = false
                }
            }
        }
}
