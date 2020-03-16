package eu.kanade.tachiyomi.ui.library

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.setBottomEdge
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.display_bottom_sheet.*
import uy.kohesive.injekt.injectLazy

class DisplayBottomSheet(private val controller: LibraryController) : BottomSheetDialog
    (controller.activity!!, R.style.BottomSheetDialogTheme) {

    val activity = controller.activity!!

    /**
     * Preferences helper.
     */
    private val preferences by injectLazy<PreferencesHelper>()

    private var sheetBehavior: BottomSheetBehavior<*>

    init {
        // Use activity theme for this layout
        val view = activity.layoutInflater.inflate(R.layout.display_bottom_sheet, null)
        setContentView(view)

        sheetBehavior = BottomSheetBehavior.from(view.parent as ViewGroup)
        setEdgeToEdge(activity, bottom_sheet, view, false)
        val height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
        } else 0
        sheetBehavior.peekHeight = 220.dpToPx + height

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) { }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.skipCollapsed = true
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGeneralPreferences()
        setBottomEdge(unread_badge_group, activity)
        close_button.setOnClickListener {
            dismiss()
            true
        }
        settings_scroll_view.viewTreeObserver.addOnGlobalLayoutListener {
            val isScrollable =
                settings_scroll_view!!.height < bottom_sheet.height +
                    settings_scroll_view.paddingTop + settings_scroll_view.paddingBottom
            close_button.visibleIf(isScrollable)
        }
    }

    private fun initGeneralPreferences() {
        display_group.bindToPreference(preferences.libraryLayout()) {
            controller.reattachAdapter()
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                dismiss()
        }
        uniform_grid.bindToPreference(preferences.uniformGrid()) {
            controller.reattachAdapter()
        }
        grid_size_toggle_group.bindToPreference(preferences.gridSize()) {
            controller.reattachAdapter()
        }
        download_badge.bindToPreference(preferences.downloadBadge()) {
            controller.presenter.requestDownloadBadgesUpdate()
        }
        unread_badge_group.bindToPreference(preferences.unreadBadgeType()) {
            controller.presenter.requestUnreadBadgesUpdate()
        }
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    private fun CompoundButton.bindToPreference(pref: Preference<Boolean>, block: () -> Unit) {
        isChecked = pref.getOrDefault()
        setOnCheckedChangeListener { _, isChecked ->
            pref.set(isChecked)
            block()
        }
    }

    /**
     * Binds a radio group with a int preference.
     */
    private fun RadioGroup.bindToPreference(pref: Preference<Int>, block: () -> Unit) {
        (getChildAt(pref.getOrDefault()) as RadioButton).isChecked = true
        setOnCheckedChangeListener { _, checkedId ->
            val index = indexOfChild(findViewById(checkedId))
            pref.set(index)
            block()
        }
    }
}
