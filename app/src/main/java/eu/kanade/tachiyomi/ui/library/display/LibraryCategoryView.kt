package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.LibraryCategoryLayoutBinding
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet.Filters.Companion.DEFAULT_ORDER
import eu.kanade.tachiyomi.ui.library.filter.ManageFilterItem
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

            reorderFiltersButton.setOnClickListener {
                val recycler = RecyclerView(context)
                var filterOrder = preferences.filterOrder().get()
                if (filterOrder.count() != 6) {
                    filterOrder = DEFAULT_ORDER
                }
                val adapter = FlexibleAdapter(
                    filterOrder.toCharArray().map(::ManageFilterItem),
                    this,
                    true
                )
                recycler.layoutManager = LinearLayoutManager(context)
                recycler.adapter = adapter
                adapter.isHandleDragEnabled = true
                adapter.isLongPressDragEnabled = true
                MaterialDialog(context).title(R.string.reorder_filters)
                    .customView(view = recycler, scrollable = false)
                    .negativeButton(android.R.string.cancel)
                    .positiveButton(R.string.reorder) {
                        val order = adapter.currentItems.map { it.char }.joinToString("")
                        preferences.filterOrder().set(order)
                        recycler.adapter = null
                    }
                    .show()
            }
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
