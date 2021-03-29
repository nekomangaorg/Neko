package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.databinding.LibraryDisplayLayoutBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView

class LibraryDisplayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryDisplayLayoutBinding>(context, attrs) {

    override fun inflateBinding() = LibraryDisplayLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.displayGroup.bindToPreference(preferences.libraryLayout()) {
            controller.reattachAdapter()
        }
        binding.uniformGrid.bindToPreference(preferences.uniformGrid()) {
            controller.reattachAdapter()
        }
        binding.gridSizeToggleGroup.bindToPreference(preferences.gridSize()) {
            controller.reattachAdapter()
        }
    }
}
