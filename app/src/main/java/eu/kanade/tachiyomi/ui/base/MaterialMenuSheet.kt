package eu.kanade.tachiyomi.ui.base

import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.checkHeightThen
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.widget.E2EBottomSheetDialog
import kotlin.math.max
import kotlin.math.min
import org.nekomanga.R
import org.nekomanga.databinding.BottomMenuSheetBinding

class MaterialMenuSheet(
    activity: Activity,
    private val items: List<MenuSheetItem>,
    title: String? = null,
    selectedId: Int? = null,
    maxHeight: Int? = null,
    showDivider: Boolean = false,
    onMenuItemClicked: (MaterialMenuSheet, Int) -> Boolean,
) : E2EBottomSheetDialog<BottomMenuSheetBinding>(activity) {

    override fun createBinding(inflater: LayoutInflater) = BottomMenuSheetBinding.inflate(inflater)

    private val fastAdapter: FastAdapter<MaterialMenuSheetItem>
    private val itemAdapter = ItemAdapter<MaterialMenuSheetItem>()

    override var recyclerView: RecyclerView? = binding.menuSheetRecycler

    init {
        binding.menuSheetLayout.checkHeightThen {
            binding.menuSheetRecycler.updateLayoutParams<ConstraintLayout.LayoutParams> {
                val fullHeight = activity.window.decorView.height
                val insets =
                    activity.window.decorView.rootWindowInsetsCompat?.getInsets(systemBars())
                matchConstraintMaxHeight =
                    min(
                        (maxHeight ?: fullHeight) + (insets?.bottom ?: 0),
                        fullHeight - (insets?.top ?: 0) - binding.titleLayout.height - 26.dpToPx,
                    )
            }
        }

        binding.divider.isVisible = showDivider

        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)
        itemAdapter.set(items.map(::MaterialMenuSheetItem))

        binding.menuSheetRecycler.layoutManager = LinearLayoutManager(context)
        binding.menuSheetRecycler.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item, _ ->
            val shouldDismiss = onMenuItemClicked(this@MaterialMenuSheet, item.sheetItem.id)
            if (shouldDismiss) {
                dismiss()
            }
            false
        }

        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true

        binding.menuSheetRecycler.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        binding.titleLayout.isVisible = title != null
        binding.toolbarTitle.text = title

        if (selectedId != null) {
            val pos = max(items.indexOfFirst { it.id == selectedId }, 0)
            itemAdapter.getAdapterItem(pos).isSelected = true
            binding.root.post {
                binding.root.post {
                    binding.menuSheetRecycler.scrollBy(
                        0,
                        pos * 48.dpToPx - binding.menuSheetRecycler.height / 2,
                    )
                }
            }
        }

        var isElevated = false
        var elevationAnimator: ValueAnimator? = null

        fun elevate(elevate: Boolean) {
            elevationAnimator?.cancel()
            isElevated = elevate
            elevationAnimator?.cancel()
            val nonElevateColor = activity.getResourceColor(R.attr.colorSurface)
            val elevateColor = activity.getResourceColor(R.attr.colorPrimaryVariant)

            elevationAnimator =
                ValueAnimator.ofArgb(
                    if (elevate) nonElevateColor else elevateColor,
                    if (elevate) elevateColor else nonElevateColor,
                )

            elevationAnimator?.addUpdateListener {
                binding.titleLayout.backgroundTintList =
                    ColorStateList.valueOf(it.animatedValue as Int)
            }
            elevationAnimator?.start()
        }
        elevate(binding.menuSheetRecycler.canScrollVertically(-1))
        if (binding.titleLayout.isVisible) {
            binding.menuSheetRecycler.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val notAtTop = binding.menuSheetRecycler.canScrollVertically(-1)
                        if (notAtTop != isElevated) {
                            elevate(notAtTop)
                        }
                    }
                },
            )
        }
    }

    private fun clearEndDrawables() {
        itemAdapter.adapterItems.forEach { it.isSelected = false }
    }

    fun setDrawable(id: Int, @DrawableRes drawableRes: Int, clearAll: Boolean = true) {
        if (clearAll) {
            clearEndDrawables()
        }
        val pos = max(items.indexOfFirst { it.id == id }, 0)
        val item = itemAdapter.getAdapterItem(pos)
        item.sheetItem.endDrawableRes = drawableRes
        item.isSelected = true
        fastAdapter.notifyAdapterDataSetChanged()
    }

    data class MenuSheetItem(
        val id: Int,
        @DrawableRes val drawable: Int = 0,
        @StringRes val textRes: Int = 0,
        val text: String? = null,
        var endDrawableRes: Int = 0,
    )
}
