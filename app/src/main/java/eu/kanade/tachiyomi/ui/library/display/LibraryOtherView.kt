package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import kotlinx.android.synthetic.main.library_other_layout.view.*

class LibraryOtherView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LibraryPreferenceView(context, attrs) {

    override fun initGeneralPreferences() {
        show_all.bindToPreference(preferences.showAllCategories()) {
            controller.presenter.getLibrary()
            category_show.isEnabled = it
        }
        category_show.isEnabled = show_all.isChecked
        category_show.bindToPreference(preferences.showCategoryInTitle()) {
            controller.showMiniBar()
        }
        hide_hopper.bindToPreference(preferences.hideHopper()) {
            controller.hideHopper(it)
        }
        hide_filters.bindToPreference(preferences.hideFiltersAtStart())
    }
}