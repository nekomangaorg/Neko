package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.databinding.LibraryCategoryLayoutBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView

class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryCategoryLayoutBinding>(context, attrs) {

    override fun inflateBinding() = LibraryCategoryLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.showAll.bindToPreference(preferences.showAllCategories()) {
            controller.presenter.getLibrary()
            binding.categoryShow.isEnabled = it
        }
        binding.categoryShow.isEnabled = binding.showAll.isChecked
        binding.categoryShow.bindToPreference(preferences.showCategoryInTitle()) {
            controller.showMiniBar()
        }
        binding.hideHopper.bindToPreference(preferences.hideHopper()) {
            controller.hideHopper(it)
        }
        binding.autoHideHopper.bindToPreference(preferences.autohideHopper()) {
            controller.resetHopperY()
        }
        binding.addCategoriesButton.setOnClickListener {
            controller.showCategoriesController()
        }
    }
}
