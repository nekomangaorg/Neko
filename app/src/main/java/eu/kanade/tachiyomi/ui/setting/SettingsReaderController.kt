package eu.kanade.tachiyomi.ui.setting

import android.os.Build
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PageLayout
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.view.activityBinding
import kotlinx.coroutines.flow.launchIn
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsReaderController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.reader

        preferenceCategory {
            titleRes = R.string.general
            intListPreference(activity) {
                key = Keys.defaultViewer
                titleRes = R.string.default_reading_mode
                entriesRes = arrayOf(
                    R.string.left_to_right_viewer,
                    R.string.right_to_left_viewer,
                    R.string.vertical_viewer,
                    R.string.webtoon,
                    R.string.continuous_vertical
                )
                entryRange = 1..5
                defaultValue = 2
            }
            intListPreference(activity) {
                key = Keys.doubleTapAnimationSpeed
                titleRes = R.string.double_tap_anim_speed
                entries = listOf(
                    context.getString(R.string.no_animation),
                    context.getString(
                        R.string.fast
                    ),
                    context.getString(R.string.normal)
                )
                entryValues = listOf(1, 250, 500) // using a value of 0 breaks the image viewer, so
                // min is 1
                defaultValue = 500
            }
            switchPreference {
                key = Keys.enableTransitions
                titleRes = R.string.animate_page_transitions
                defaultValue = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                switchPreference {
                    key = Keys.trueColor
                    titleRes = R.string.true_32bit_color
                    summaryRes = R.string.reduces_banding_impacts_performance
                    defaultValue = false
                }
            }
            intListPreference(activity) {
                key = Keys.preloadSize
                titleRes = R.string.page_preload_amount
                entryValues = listOf(4, 6, 8, 10, 12, 14, 16, 20)
                entries = entryValues.map { context.resources.getQuantityString(R.plurals.pages_plural, it, it) }
                defaultValue = 6
                summaryRes = R.string.amount_of_pages_to_preload
            }
            multiSelectListPreferenceMat(activity) {
                key = Keys.readerBottomButtons
                titleRes = R.string.display_buttons_bottom_reader
                val enumConstants = ReaderActivity.BottomButton::class.java.enumConstants
                entriesRes = enumConstants?.map { it.stringRes }.orEmpty().toTypedArray()
                entryValues = enumConstants?.map { it.value }.orEmpty()
                allSelectionRes = R.string.display_options
                allIsAlwaysSelected = true
                showAllLast = true
                defaultValue = ReaderActivity.BUTTONS_DEFAULTS
            }
            infoPreference(R.string.certain_buttons_can_be_found)
        }

        preferenceCategory {
            titleRes = R.string.display

            intListPreference(activity) {
                key = Keys.rotation
                titleRes = R.string.rotation
                entriesRes = arrayOf(
                    R.string.free,
                    R.string.lock,
                    R.string.force_portrait,
                    R.string.force_landscape
                )
                entryRange = 1..4
                defaultValue = 1
            }
            intListPreference(activity) {
                key = Keys.readerTheme
                titleRes = R.string.background_color
                entriesRes = arrayOf(
                    R.string.white,
                    R.string.black,
                    R.string.smart_based_on_page,
                    R.string.smart_based_on_page_and_theme
                )
                entryRange = 0..3
                defaultValue = 2
            }
            switchPreference {
                key = Keys.fullscreen
                titleRes = R.string.fullscreen
                defaultValue = true
            }
            switchPreference {
                key = Keys.keepScreenOn
                titleRes = R.string.keep_screen_on
                defaultValue = true
            }
            switchPreference {
                key = Keys.showPageNumber
                titleRes = R.string.show_page_number
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = R.string.reading

            switchPreference {
                key = Keys.skipRead
                titleRes = R.string.skip_read_chapters
                defaultValue = false
            }
            switchPreference {
                key = Keys.skipFiltered
                titleRes = R.string.skip_filtered_chapters
                defaultValue = true
            }
            switchPreference {
                key = Keys.alwaysShowChapterTransition
                titleRes = R.string.always_show_chapter_transition
                summaryRes = R.string.if_disabled_transition_will_skip
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = R.string.paged

            intListPreference(activity) {
                key = Keys.navigationModePager
                titleRes = R.string.nav_layout
                entries = context.resources.getStringArray(R.array.reader_nav).also { values ->
                    entryRange = 0..values.size
                }.toList()
                defaultValue = "0"

                preferences.readWithTapping().asImmediateFlow { isVisible = it }.launchIn(viewScope)
            }
            listPreference(activity) {
                key = Keys.pagerNavInverted
                titleRes = R.string.invert_tapping
                entriesRes = arrayOf(
                    R.string.none,
                    R.string.horizontally,
                    R.string.vertically,
                    R.string.both_axes
                )
                entryValues = listOf(
                    ViewerNavigation.TappingInvertMode.NONE.name,
                    ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                    ViewerNavigation.TappingInvertMode.VERTICAL.name,
                    ViewerNavigation.TappingInvertMode.BOTH.name
                )
                defaultValue = ViewerNavigation.TappingInvertMode.NONE.name

                preferences.readWithTapping().asImmediateFlow { isVisible = it }.launchIn(viewScope)
            }

            intListPreference(activity) {
                key = Keys.imageScaleType
                titleRes = R.string.scale_type
                entriesRes = arrayOf(
                    R.string.fit_screen,
                    R.string.stretch,
                    R.string.fit_width,
                    R.string.fit_height,
                    R.string.original_size,
                    R.string.smart_fit
                )
                entryRange = 1..6
                defaultValue = 1
            }

            intListPreference(activity) {
                key = Keys.pagerCutoutBehavior
                titleRes = R.string.cutout_area_behavior
                entriesRes = arrayOf(
                    R.string.pad_cutout_areas,
                    R.string.start_past_cutout,
                    R.string.ignore_cutout_areas,
                )
                summaryRes = R.string.cutout_behavior_only_applies
                entryRange = 0..2
                defaultValue = 0
                // Calling this once to show only on cutout
                isVisible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    activityBinding?.root?.rootWindowInsets?.displayCutout?.safeInsetTop != null ||
                        activityBinding?.root?.rootWindowInsets?.displayCutout?.safeInsetBottom != null
                } else {
                    false
                }
                // Calling this a second time in case activity is recreated while on this page
                // Keep the first so it shouldn't animate hiding the preference for phones without
                // cutouts
                activityBinding?.root?.post {
                    isVisible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        activityBinding?.root?.rootWindowInsets?.displayCutout?.safeInsetTop != null ||
                            activityBinding?.root?.rootWindowInsets?.displayCutout?.safeInsetBottom != null
                    } else {
                        false
                    }
                }
            }

            intListPreference(activity) {
                key = Keys.zoomStart
                titleRes = R.string.zoom_start_position
                entriesRes = arrayOf(
                    R.string.automatic,
                    R.string.left,
                    R.string.right,
                    R.string.center
                )
                entryRange = 1..4
                defaultValue = 1
            }
            switchPreference {
                key = Keys.cropBorders
                titleRes = R.string.crop_borders
                defaultValue = false
            }
            intListPreference(activity) {
                key = Keys.pageLayout
                title = context.getString(R.string.page_layout).addBetaTag(context)
                dialogTitleRes = R.string.page_layout
                entriesRes = arrayOf(
                    R.string.single_page,
                    R.string.double_pages,
                    R.string.automatic_orientation
                )
                entryRange = 0..2
                defaultValue = 2
            }
            infoPreference(R.string.automatic_can_still_switch).apply {
                preferences.pageLayout().asImmediateFlow(viewScope) { isVisible = it == PageLayout.AUTOMATIC }
            }
            switchPreference {
                key = Keys.invertDoublePages
                titleRes = R.string.invert_double_pages
                defaultValue = false
                preferences.pageLayout().asImmediateFlow(viewScope) { isVisible = it != PageLayout.SINGLE_PAGE }
            }
        }
        preferenceCategory {
            titleRes = R.string.webtoon

            intListPreference(activity) {
                key = Keys.navigationModeWebtoon
                titleRes = R.string.nav_layout
                entries = context.resources.getStringArray(R.array.reader_nav).also { values ->
                    entryRange = 0..values.size
                }.toList()
                defaultValue = "0"

                preferences.readWithTapping().asImmediateFlow { isVisible = it }.launchIn(viewScope)
            }
            listPreference(activity) {
                key = Keys.webtoonNavInverted
                titleRes = R.string.invert_tapping
                entriesRes = arrayOf(
                    R.string.none,
                    R.string.horizontally,
                    R.string.vertically,
                    R.string.both_axes
                )
                entryValues = listOf(
                    ViewerNavigation.TappingInvertMode.NONE.name,
                    ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                    ViewerNavigation.TappingInvertMode.VERTICAL.name,
                    ViewerNavigation.TappingInvertMode.BOTH.name
                )
                defaultValue = ViewerNavigation.TappingInvertMode.NONE.name

                preferences.readWithTapping().asImmediateFlow { isVisible = it }.launchIn(viewScope)
            }

            switchPreference {
                key = Keys.cropBordersWebtoon
                titleRes = R.string.crop_borders
                defaultValue = false
            }

            intListPreference(activity) {
                key = Keys.webtoonSidePadding
                titleRes = R.string.pref_webtoon_side_padding
                entriesRes = arrayOf(
                    R.string.webtoon_side_padding_0,
                    R.string.webtoon_side_padding_10,
                    R.string.webtoon_side_padding_15,
                    R.string.webtoon_side_padding_20,
                    R.string.webtoon_side_padding_25
                )
                entryValues = listOf(0, 10, 15, 20, 25)
                defaultValue = "0"
            }

            switchPreference {
                key = Keys.webtoonEnableZoomOut
                titleRes = R.string.enable_zoom_out
                defaultValue = false
            }
        }
        preferenceCategory {
            titleRes = R.string.navigation

            switchPreference {
                key = Keys.readWithTapping
                titleRes = R.string.tapping
                defaultValue = true
            }
            switchPreference {
                key = Keys.readWithLongTap
                titleRes = R.string.long_tap_dialog
                defaultValue = true
            }
            switchPreference {
                key = Keys.readWithVolumeKeys
                titleRes = R.string.volume_keys
                defaultValue = false
            }
            switchPreference {
                key = Keys.readWithVolumeKeysInverted
                titleRes = R.string.invert_volume_keys
                defaultValue = false

                preferences.readWithVolumeKeys().asImmediateFlow { isVisible = it }.launchIn(viewScope)
            }
        }
    }
}
