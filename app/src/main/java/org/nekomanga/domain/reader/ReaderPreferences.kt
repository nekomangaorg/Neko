package org.nekomanga.domain.reader

import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import org.nekomanga.core.preferences.PreferenceValues
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum

class ReaderPreferences(private val preferenceStore: PreferenceStore) {

    fun animatedPageTransitions() =
        this.preferenceStore.getBoolean("pref_enable_transitions_key", true)

    fun animatedPageTransitionsWebtoon() =
        this.preferenceStore.getBoolean("pref_enable_transitions_webtoon_key", true)

    fun pagerCutoutBehavior() = this.preferenceStore.getInt("pager_cutout_behavior")

    fun doubleTapAnimSpeed() = this.preferenceStore.getInt("pref_double_tap_anim_speed", 500)

    fun showPageNumber() = this.preferenceStore.getBoolean("pref_show_page_number_key", true)

    fun trueColor() = this.preferenceStore.getBoolean("pref_true_color_key")

    fun fullscreen() = this.preferenceStore.getBoolean("fullscreen", true)

    fun keepScreenOn() = this.preferenceStore.getBoolean("pref_keep_screen_on_key", true)

    fun customBrightness() = this.preferenceStore.getBoolean("pref_custom_brightness_key")

    fun customBrightnessValue() = this.preferenceStore.getInt("custom_brightness_value")

    fun colorFilter() = this.preferenceStore.getBoolean("pref_color_filter_key")

    fun colorFilterValue() = this.preferenceStore.getInt("color_filter_value")

    fun colorFilterMode() = this.preferenceStore.getInt("color_filter_mode")

    fun defaultReadingMode() =
        this.preferenceStore.getInt(
            "pref_default_reading_mode_key",
            ReadingModeType.RIGHT_TO_LEFT.flagValue,
        )

    fun defaultOrientationType() =
        this.preferenceStore.getInt(
            "pref_default_orientation_type_key",
            OrientationType.FREE.flagValue,
        )

    fun imageScaleType() = this.preferenceStore.getInt("pref_image_scale_type_key", 1)

    fun doublePageGap() = this.preferenceStore.getInt("double_page_gap")

    fun zoomStart() = this.preferenceStore.getInt("pref_zoom_start_key", 1)

    fun readerTheme() = this.preferenceStore.getInt("pref_reader_theme_key", 2)

    fun cropBorders() = this.preferenceStore.getBoolean("crop_borders")

    fun cropBordersWebtoon() = this.preferenceStore.getBoolean("crop_borders_webtoon")

    fun navigateToPan() = this.preferenceStore.getBoolean("navigate_pan", true)

    fun landscapeZoom() = this.preferenceStore.getBoolean("landscape_zoom")

    fun grayscale() = this.preferenceStore.getBoolean("pref_grayscale")

    fun colorEInk16bit() = this.preferenceStore.getBoolean("pref_eink_16bit", false)

    fun colorEInkDither() = this.preferenceStore.getBoolean("pref_eink_dither", true)

    fun invertedColors() = this.preferenceStore.getBoolean("pref_inverted_colors")

    fun webtoonSidePadding() = this.preferenceStore.getInt("webtoon_side_padding")

    fun webtoonEnableZoomOut() = this.preferenceStore.getBoolean("webtoon_enable_zoom_out")

    fun folderPerManga() = this.preferenceStore.getBoolean("create_folder_per_manga", false)

    fun readWithLongTap() = this.preferenceStore.getBoolean("reader_long_tap", true)

    fun readWithVolumeKeys() = this.preferenceStore.getBoolean("reader_volume_keys")

    fun readWithVolumeKeysInverted() =
        this.preferenceStore.getBoolean("reader_volume_keys_inverted")

    fun navigationModePager() = this.preferenceStore.getInt("reader_navigation_mode_pager")

    fun navigationModeWebtoon() = this.preferenceStore.getInt("reader_navigation_mode_webtoon")

    fun pagerNavInverted() =
        this.preferenceStore.getEnum(
            "reader_tapping_inverted",
            ViewerNavigation.TappingInvertMode.NONE,
        )

    fun webtoonNavInverted() =
        this.preferenceStore.getEnum(
            "reader_tapping_inverted_webtoon",
            ViewerNavigation.TappingInvertMode.NONE,
        )

    fun pageLayout() = this.preferenceStore.getInt("page_layout", PageLayout.AUTOMATIC.value)

    fun automaticSplitsPage() = this.preferenceStore.getBoolean("automatic_splits_page")

    fun invertDoublePages() = this.preferenceStore.getBoolean("invert_double_pages")

    fun webtoonPageLayout() =
        this.preferenceStore.getInt("webtoon_page_layout", PageLayout.SINGLE_PAGE.webtoonValue)

    fun webtoonReaderHideThreshold() =
        this.preferenceStore.getEnum(
            "reader_hide_threshold",
            PreferenceValues.ReaderHideThreshold.LOW,
        )

    fun webtoonInvertDoublePages() = this.preferenceStore.getBoolean("webtoon_invert_double_pages")

    fun readerBottomButtons() =
        this.preferenceStore.getStringSet(
            "reader_bottom_buttons",
            ReaderBottomButton.BUTTONS_DEFAULTS,
        )

    fun preloadPageAmount() = this.preferenceStore.getInt("preload_size", 6)

    fun splitTallImages() = this.preferenceStore.getBoolean("split_tall_images")

    fun doublePageRotate() = this.preferenceStore.getBoolean("double_page_rotate")

    fun doublePageRotateReverse() = this.preferenceStore.getBoolean("double_page_rotate_reverse")

    fun skipRead() = this.preferenceStore.getBoolean("skip_read")

    fun skipFiltered() = this.preferenceStore.getBoolean("skip_filtered", true)

    fun skipDuplicates() = this.preferenceStore.getBoolean("skip_duplicates")

    fun alwaysShowChapterTransition() =
        this.preferenceStore.getBoolean("always_show_chapter_transition", true)
}
