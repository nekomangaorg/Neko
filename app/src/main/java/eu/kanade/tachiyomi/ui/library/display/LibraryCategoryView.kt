package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.LibraryCategoryLayoutBinding
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.system.toInt
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import kotlin.math.min

class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryCategoryLayoutBinding>(context, attrs) {

    override fun inflateBinding() = LibraryCategoryLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        with(binding) {
            showAll.bindToPreference(preferences.showAllCategories()) {
                controller?.presenter?.getLibrary()
                binding.categoryShow.isEnabled = it
            }
            categoryShow.isEnabled = showAll.isChecked
            categoryShow.bindToPreference(preferences.showCategoryInTitle()) {
                controller?.showMiniBar()
            }
            val hideHopper = min(
                2,
                preferences.hideHopper().get().toInt() * 2 + preferences.autohideHopper().get()
                    .toInt()
            )
            hideHopperSpinner.setSelection(hideHopper)
            hideHopperSpinner.onItemSelectedListener = {
                preferences.hideHopper().set(it == 2)
                preferences.autohideHopper().set(it == 1)
                controller?.hideHopper(it == 2)
                controller?.resetHopperY()
            }
            addCategoriesButton.setOnClickListener {
                controller?.showCategoriesController()
            }
            expandCollapseCategories.setOnClickListener {
                controller?.binding?.filterBottomSheet?.root
                    ?.onGroupClicked?.invoke(FilterBottomSheet.ACTION_EXPAND_COLLAPSE_ALL)
            }
            hopperLongPress.bindToPreference(preferences.hopperLongPressAction())
        }
    }

    fun showExpandCategories(show: Boolean) {
        binding.expandCollapseCategories.isVisible = show
    }

    fun setExpandText(expand: Boolean, animated: Boolean = true) {
        binding.expandCollapseCategories.setText(
            if (expand) {
                R.string.expand_all_categories
            } else {
                R.string.collapse_all_categories
            }
        )
        if (animated) {
            binding.expandCollapseCategories.setIconResource(
                if (expand) {
                    R.drawable.anim_expand_less_to_more
                } else {
                    R.drawable.anim_expand_more_to_less
                }
            )
            (binding.expandCollapseCategories.icon as? AnimatedVectorDrawable)?.start()
        } else {
            binding.expandCollapseCategories.setIconResource(
                if (expand) {
                    R.drawable.ic_expand_more_24dp
                } else {
                    R.drawable.ic_expand_less_24dp
                }
            )
        }
    }
}
