package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.LibraryDisplayLayoutBinding
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.library.filter.ManageFilterItem
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.system.bottomCutoutInset
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.isLandscape
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.topCutoutInset
import eu.kanade.tachiyomi.util.view.checkHeightThen
import eu.kanade.tachiyomi.util.view.numberOfRowsForValue
import eu.kanade.tachiyomi.util.view.rowsForValue
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import kotlin.math.roundToInt

class LibraryDisplayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryDisplayLayoutBinding>(context, attrs) {

    var mainView: View? = null
    override fun inflateBinding() = LibraryDisplayLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.displayGroup.bindToPreference(preferences.libraryLayout())
        binding.uniformGrid.bindToPreference(preferences.uniformGrid()) {
            binding.staggeredGrid.isEnabled = !it
        }
        binding.outlineOnCovers.bindToPreference(preferences.outlineOnCovers())
        binding.staggeredGrid.text = context.getString(R.string.use_staggered_grid).addBetaTag(context)
        binding.staggeredGrid.isEnabled = !preferences.uniformGrid().get()
        binding.staggeredGrid.bindToPreference(preferences.useStaggeredGrid())
        binding.gridSeekbar.value = ((preferences.gridSize().get() + .5f) * 2f).roundToInt().toFloat()
        binding.resetGridSize.setOnClickListener {
            binding.gridSeekbar.value = 3f
        }

        binding.reorderFiltersButton.setOnClickListener {
            val recycler = RecyclerView(context)
            var filterOrder = preferences.filterOrder().get()
            if (filterOrder.count() != 6) {
                filterOrder = FilterBottomSheet.Filters.DEFAULT_ORDER
            }
            val adapter = FlexibleAdapter(
                filterOrder.toCharArray().map {
                    if (FilterBottomSheet.Filters.filterOf(it) != null) {
                        ManageFilterItem(it)
                    } else {
                        null
                    }
                }.filterNotNull(),
                this,
                true,
            )
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.adapter = adapter
            adapter.isHandleDragEnabled = true
            adapter.isLongPressDragEnabled = true
            context.materialAlertDialog()
                .setTitle(R.string.reorder_filters)
                .setView(recycler)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reorder) { _, _ ->
                    val order = adapter.currentItems.map { it.char }.joinToString("")
                    preferences.filterOrder().set(order)
                    recycler.adapter = null
                }
                .show()
        }

        binding.gridSeekbar.setLabelFormatter {
            val view = controller?.activity?.window?.decorView ?: mainView ?: this@LibraryDisplayView
            val mainText = (mainView ?: this@LibraryDisplayView).rowsForValue(it).toString()
            val mainOrientation = context.getString(
                if (context.isLandscape()) {
                    R.string.landscape
                } else {
                    R.string.portrait
                },
            )
            val alt = (
                if (view.measuredHeight >= 720.dpToPx) {
                    view.measuredHeight - 72.dpToPx
                } else {
                    view.measuredHeight
                }
                ) -
                (view.rootWindowInsets?.topCutoutInset() ?: 0) -
                (view.rootWindowInsets?.bottomCutoutInset() ?: 0)
            val altText = alt.numberOfRowsForValue(it).toString()
            val altOrientation = context.getString(
                if (context.isLandscape()) {
                    R.string.portrait
                } else {
                    R.string.landscape
                },
            )
            "$mainOrientation: $mainText • $altOrientation: $altText"
        }
        binding.gridSeekbar.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) {
                preferences.gridSize().set((value / 2f) - .5f)
            }
            setGridText(value)
        }
        binding.gridSeekbar.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}

                override fun onStopTrackingTouch(slider: Slider) {
                    preferences.gridSize().set((slider.value / 2f) - .5f)
                    setGridText(slider.value)
                }
            },
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        checkHeightThen {
            setGridText(binding.gridSeekbar.value)
        }
    }

    private fun setGridText(progress: Float) {
        with(binding.gridSizeText) {
            val rows = (mainView ?: this@LibraryDisplayView).rowsForValue(progress)
            val titleText = context.getString(R.string.grid_size)
            val subtitleText = context.getString(R.string._per_row, rows)
            text = titleText.withSubtitle(context, subtitleText)
        }
    }
}
