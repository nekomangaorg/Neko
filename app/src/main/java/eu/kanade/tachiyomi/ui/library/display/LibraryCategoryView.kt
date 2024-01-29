package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.system.toInt
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import kotlin.math.min
import org.nekomanga.R
import org.nekomanga.databinding.LibraryCategoryLayoutBinding

class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryCategoryLayoutBinding>(context, attrs) {

    override fun inflateBinding() = LibraryCategoryLayoutBinding.bind(this)

    override fun initGeneralPreferences() {
        with(binding) {
            showAll.bindToPreference(libraryPreferences.showAllCategories()) {
                controller?.presenter?.getLibrary()
                binding.categoryShow.isEnabled = it
            }
            categoryShow.isEnabled = showAll.isChecked
            categoryShow.bindToPreference(libraryPreferences.showCategoryInTitle()) {
                controller?.showMiniBar()
            }
            dynamicToBottom.text =
                context
                    .getString(R.string.move_dynamic_to_bottom)
                    .withSubtitle(context, R.string.when_grouping_by_sources_tags)
            dynamicToBottom.bindToPreference(libraryPreferences.collapsedDynamicAtBottom()) {
                controller?.presenter?.getLibrary()
            }
            showEmptyCatsFiltering.bindToPreference(
                libraryPreferences.showEmptyCategoriesWhileFiltering()
            ) {
                controller?.presenter?.requestFilterUpdate()
            }
            val hideHopper =
                min(
                    2,
                    libraryPreferences.hideHopper().get().toInt() * 2 +
                        libraryPreferences.autoHideHopper().get().toInt(),
                )
            hideHopperSpinner.setSelection(hideHopper)
            hideHopperSpinner.onItemSelectedListener = {
                libraryPreferences.hideHopper().set(it == 2)
                libraryPreferences.autoHideHopper().set(it == 1)
                controller?.hideHopper(it == 2)
                controller?.resetHopperY()
            }
            addCategoriesButton.setOnClickListener { controller?.showCategoriesController() }
            hopperLongPress.bindToPreference(libraryPreferences.hopperLongPressAction())
        }
    }
}
