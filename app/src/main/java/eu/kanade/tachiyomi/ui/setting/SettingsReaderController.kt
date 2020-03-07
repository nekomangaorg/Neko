package eu.kanade.tachiyomi.ui.setting

import android.os.Build
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsReaderController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_reader

        intListPreference(activity) {
            key = Keys.defaultViewer
            titleRes = R.string.pref_viewer_type
            entriesRes = arrayOf(R.string.left_to_right_viewer, R.string.right_to_left_viewer,
                    R.string.vertical_viewer, R.string.webtoon)
            entryRange = 1..4
            defaultValue = 1
        }
        intListPreference(activity) {
            key = Keys.imageScaleType
            titleRes = R.string.pref_image_scale_type
            entriesRes = arrayOf(R.string.scale_type_fit_screen, R.string.scale_type_stretch,
                    R.string.scale_type_fit_width, R.string.scale_type_fit_height,
                    R.string.scale_type_original_size, R.string.scale_type_smart_fit)
            entryRange = 1..6
            defaultValue = 1
        }
        intListPreference(activity) {
            key = Keys.zoomStart
            titleRes = R.string.pref_zoom_start
            entriesRes = arrayOf(R.string.zoom_start_automatic, R.string.zoom_start_left,
                    R.string.zoom_start_right, R.string.zoom_start_center)
            entryRange = 1..4
            defaultValue = 1
        }
        intListPreference(activity) {
            key = Keys.rotation
            titleRes = R.string.pref_rotation_type
            entriesRes = arrayOf(R.string.rotation_free, R.string.rotation_lock,
                    R.string.rotation_force_portrait, R.string.rotation_force_landscape)
            entryRange = 1..4
            defaultValue = 1
        }
        intListPreference(activity) {
            key = Keys.readerTheme
            titleRes = R.string.pref_reader_theme
            entriesRes = arrayOf(R.string.white_background, R.string.black_background, R.string
                .reader_theme_smart, R.string.reader_theme_smart_theme)
            entryRange = 0..3
            defaultValue = 2
        }
        intListPreference(activity) {
            key = Keys.doubleTapAnimationSpeed
            titleRes = R.string.pref_double_tap_anim_speed
            entries = listOf(context.getString(R.string.double_tap_anim_speed_0), context.getString(R
                .string.double_tap_anim_speed_fast), context.getString(R.string.double_tap_anim_speed_normal))
            entryValues = listOf(1, 250, 500) // using a value of 0 breaks the image viewer, so
            // min is 1
            defaultValue = 500
        }
        switchPreference {
            key = Keys.skipRead
            titleRes = R.string.pref_skip_read_chapters
            defaultValue = false
        }
        switchPreference {
            key = Keys.fullscreen
            titleRes = R.string.pref_fullscreen
            defaultValue = true
        }
        switchPreference {
            key = Keys.keepScreenOn
            titleRes = R.string.pref_keep_screen_on
            defaultValue = true
        }
        switchPreference {
            key = Keys.showPageNumber
            titleRes = R.string.pref_show_page_number
            defaultValue = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switchPreference {
                key = Keys.trueColor
                titleRes = R.string.pref_true_color
                defaultValue = false
            }
        }
        preferenceCategory {
            titleRes = R.string.pager_viewer

            switchPreference {
                key = Keys.enableTransitions
                titleRes = R.string.pref_page_transitions
                defaultValue = true
            }
            switchPreference {
                key = Keys.cropBorders
                titleRes = R.string.pref_crop_borders
                defaultValue = false
            }
        }
        preferenceCategory {
            titleRes = R.string.webtoon

            switchPreference {
                key = Keys.cropBordersWebtoon
                titleRes = R.string.pref_crop_borders
                defaultValue = false
            }
        }
        preferenceCategory {
            titleRes = R.string.pref_reader_navigation

            switchPreference {
                key = Keys.readWithTapping
                titleRes = R.string.pref_read_with_tapping
                defaultValue = true
            }
            switchPreference {
                key = Keys.readWithLongTap
                titleRes = R.string.pref_read_with_long_tap
                defaultValue = true
            }
            switchPreference {
                key = Keys.readWithVolumeKeys
                titleRes = R.string.pref_read_with_volume_keys
                defaultValue = false
            }
            switchPreference {
                key = Keys.readWithVolumeKeysInverted
                titleRes = R.string.pref_read_with_volume_keys_inverted
                defaultValue = false
            }.apply { dependency = Keys.readWithVolumeKeys }
        }
    }

}
