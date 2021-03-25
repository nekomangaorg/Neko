package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import kotlinx.android.synthetic.main.library_category_layout.view.*

class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView(context, attrs) {

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
        auto_hide_hopper.bindToPreference(preferences.autohideHopper()) {
            controller.resetHopperY()
        }
    }
}
