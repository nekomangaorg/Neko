package eu.kanade.tachiyomi.ui.library.display

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.LibraryDisplayLayoutBinding
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.library.filter.ManageFilterItem
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.rowsForValue
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import eu.kanade.tachiyomi.widget.EndAnimatorListener
import kotlin.math.roundToInt

class LibraryDisplayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryDisplayLayoutBinding>(context, attrs) {

    var mainView: View? = null
    override fun inflateBinding() = LibraryDisplayLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.displayGroup.bindToPreference(preferences.libraryLayout())
        binding.uniformGrid.bindToPreference(preferences.uniformGrid())
        binding.gridSeekbar.progress = ((preferences.gridSize().get() + .5f) * 2f).roundToInt()
        binding.resetGridSize.setOnClickListener {
            binding.gridSeekbar.progress = 3
        }

        binding.reorderFiltersButton.setOnClickListener {
            val recycler = RecyclerView(context)
            var filterOrder = preferences.filterOrder().get()
            if (filterOrder.count() != 6) {
                filterOrder = FilterBottomSheet.Filters.DEFAULT_ORDER
            }
            val adapter = FlexibleAdapter(
                filterOrder.toCharArray().map {
                    if (FilterBottomSheet.Filters.filterOf(it) != null) ManageFilterItem(it)
                    else null
                }.filterNotNull(),
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

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (binding.root.width > 0) {
                    setGridText(binding.gridSeekbar.progress)
                    binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
        binding.gridSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null && fromUser) {
                    alpha = 1f
                    isVisible = true
                    adjustSeekBarTip(seekBar, progress)
                }
                if (!fromUser) {
                    preferences.gridSize().set((progress / 2f) - .5f)
                }
                setGridText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                with(binding.seekBarTextView.root) {
                    alpha = 0f
                    isVisible = true
                    animate().alpha(1f).setDuration(250L).start()
                    seekBar?.post {
                        adjustSeekBarTip(seekBar, seekBar.progress)
                    }
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                preferences.gridSize().set(((seekBar?.progress ?: 2) / 2f) - .5f)
                with(binding.seekBarTextView.root) {
                    isVisible = true
                    alpha = 1f
                    post {
                        val anim =
                            ValueAnimator.ofFloat(
                                1f,
                                0f
                            ) // animate().alpha(0f).setDuration(250L)
                        anim.duration = 250
                        anim.startDelay = 500
                        anim.addUpdateListener {
                            alpha = it.animatedValue as Float
                        }
                        anim.addListener {
                            EndAnimatorListener {
                                isVisible = false
                            }
                        }
                        anim.start()
                    }
                }
            }
        })
    }

    private fun setGridText(progress: Int) {
        with(binding.gridSizeText) {
            val rows = (mainView ?: this@LibraryDisplayView).rowsForValue(progress)
            val titleText = context.getString(R.string.grid_size)
            val subtitleText = context.getString(R.string._per_row, rows)
            text = titleText.withSubtitle(context, subtitleText)
        }
    }

    private fun adjustSeekBarTip(seekBar: SeekBar, progress: Int) {
        with(binding.seekBarTextView.root) {
            val value =
                (progress * (seekBar.width - 12.dpToPx - 2 * seekBar.thumbOffset)) / seekBar.max
            text = (mainView ?: this@LibraryDisplayView).rowsForValue(progress).toString()
            x = seekBar.x + value + seekBar.thumbOffset / 2 + 5.dpToPx
            y = seekBar.y + binding.gridSizeLayout.y - 6.dpToPx - height
        }
    }
}
