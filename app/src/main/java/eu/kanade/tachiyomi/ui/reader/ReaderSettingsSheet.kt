package eu.kanade.tachiyomi.ui.reader

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.widget.NestedScrollView
import android.widget.CompoundButton
import android.widget.Spinner
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.setBottomEdge
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.android.synthetic.main.reader_settings_sheet.*
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show reader and viewer preferences.
 */
class ReaderSettingsSheet(private val activity: ReaderActivity) : BottomSheetDialog
    (activity, R.style.BottomSheetDialogTheme) {

    /**
     * Preferences helper.
     */
    private val preferences by injectLazy<PreferencesHelper>()

    private var sheetBehavior: BottomSheetBehavior<*>

    val scroll:NestedScrollView

    init {
        // Use activity theme for this layout
        val view = activity.layoutInflater.inflate(R.layout.reader_settings_sheet, null)
        scroll = NestedScrollView(activity)
        scroll.addView(view)
        setContentView(scroll)

        sheetBehavior = BottomSheetBehavior.from(scroll.parent as ViewGroup)
        setEdgeToEdge(activity, constraint_layout, scroll,
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            preferences.readerTheme().getOrDefault() == 0 &&
            activity.window.decorView.rootWindowInsets.systemWindowInsetRight == 0 &&
            activity.window.decorView.rootWindowInsets.systemWindowInsetLeft == 0)
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
        } else 0
        sheetBehavior.peekHeight = 200.dpToPx + height

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) { }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                   sheetBehavior.skipCollapsed = true
                }
            }
        })

    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initGeneralPreferences()

        when (activity.viewer) {
            is PagerViewer -> initPagerPreferences()
            is WebtoonViewer -> initWebtoonPreferences()
        }

        setBottomEdge(
            if (activity.viewer is PagerViewer) page_transitions else crop_borders_webtoon, activity
        )

        close_button.setOnClickListener {
            dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * Init general reader preferences.
     */
    private fun initGeneralPreferences() {
        viewer.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            activity.presenter.setMangaViewer(position)
        }
        viewer.setSelection(activity.presenter.manga?.viewer ?: 0, false)

        rotation_mode.bindToPreference(preferences.rotation(), 1)
        background_color.bindToPreference(preferences.readerTheme(), 0, true)
        show_page_number.bindToPreference(preferences.showPageNumber())
        fullscreen.bindToPreference(preferences.fullscreen())
        keepscreen.bindToPreference(preferences.keepScreenOn())
        long_tap.bindToPreference(preferences.readWithLongTap())
    }

    /**
     * Init the preferences for the pager reader.
     */
    private fun initPagerPreferences() {
        pager_prefs_group.visible()
        webtoon_prefs_group.gone()
        scale_type.bindToPreference(preferences.imageScaleType(), 1)
        zoom_start.bindToPreference(preferences.zoomStart(), 1)
        crop_borders.bindToPreference(preferences.cropBorders())
        page_transitions.bindToPreference(preferences.pageTransitions())
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        webtoon_prefs_group.visible()
        pager_prefs_group.gone()
        crop_borders_webtoon.bindToPreference(preferences.cropBordersWebtoon())
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    private fun CompoundButton.bindToPreference(pref: Preference<Boolean>) {
        isChecked = pref.getOrDefault()
        setOnCheckedChangeListener { _, isChecked -> pref.set(isChecked) }
    }

    /**
     * Binds a spinner to an int preference with an optional offset for the value.
     */
    private fun Spinner.bindToPreference(pref: Preference<Int>, offset: Int = 0, shouldDismiss:
    Boolean
    = false) {
        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            pref.set(position + offset)
            if (shouldDismiss)
                dismiss()
        }
        setSelection(pref.getOrDefault() - offset, false)
    }

}
