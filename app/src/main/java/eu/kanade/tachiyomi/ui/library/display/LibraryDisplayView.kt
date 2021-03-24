package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.util.bindToPreference
import kotlinx.android.synthetic.main.library_display_layout.view.*

class LibraryDisplayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LibraryPreferenceView(context, attrs) {

    override fun initGeneralPreferences() {
        display_group.bindToPreference(preferences.libraryLayout()) {
            controller.reattachAdapter()
        }
        uniform_grid.bindToPreference(preferences.uniformGrid()) {
            controller.reattachAdapter()
        }
        grid_size_toggle_group.bindToPreference(preferences.gridSize()) {
            controller.reattachAdapter()
        }
    }
}