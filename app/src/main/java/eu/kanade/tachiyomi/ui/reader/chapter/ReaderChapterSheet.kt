package eu.kanade.tachiyomi.ui.reader.chapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderChaptersSheetBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.util.view.isExpanded
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ReaderChapterSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    var sheetBehavior: BottomSheetBehavior<View>? = null
    lateinit var viewModel: ReaderViewModel
    var adapter: FastAdapter<ReaderChapterItem>? = null
    private val itemAdapter = ItemAdapter<ReaderChapterItem>()
    var selectedChapterId = -1L

    var loadingPos = 0
    lateinit var binding: ReaderChaptersSheetBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ReaderChaptersSheetBinding.bind(this)
    }

    fun setup(activity: ReaderActivity) {
        viewModel = activity.viewModel
        val fullPrimary = activity.getResourceColor(R.attr.colorSurface)

        val primary = ColorUtils.setAlphaComponent(fullPrimary, 200)

        val hasLightNav = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || activity.isInNightMode()
        val navPrimary = ColorUtils.setAlphaComponent(
            if (hasLightNav) {
                fullPrimary
            } else {
                Color.BLACK
            },
            200,
        )
        sheetBehavior = BottomSheetBehavior.from(this)
        binding.chaptersButton.setOnClickListener {
            if (sheetBehavior.isExpanded()) {
                sheetBehavior?.collapse()
            } else {
                sheetBehavior?.expand()
            }
        }

        post {
            binding.chapterRecycler.alpha = if (sheetBehavior.isExpanded()) 1f else 0f
            binding.chapterRecycler.isClickable = sheetBehavior.isExpanded()
            binding.chapterRecycler.isFocusable = sheetBehavior.isExpanded()
            val canShowNav = viewModel.getCurrentChapter()?.pages?.size ?: 1 > 1
            if (canShowNav) {
                activity.binding.readerNav.root.isVisible = sheetBehavior.isCollapsed()
            }
        }

        sheetBehavior?.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    binding.root.isVisible = true
                    binding.pill.alpha = (1 - max(0f, progress)) * 0.25f
                    val trueProgress = max(progress, 0f)
                    activity.binding.readerNav.root.alpha = (1 - abs(progress)).coerceIn(0f, 1f)
                    backgroundTintList =
                        ColorStateList.valueOf(lerpColor(primary, fullPrimary, trueProgress))
                    binding.chapterRecycler.alpha = trueProgress
                    if (activity.sheetManageNavColor && progress > 0f) {
                        activity.window.navigationBarColor =
                            lerpColor(ColorUtils.setAlphaComponent(navPrimary, if (hasLightNav) 0 else 179), navPrimary, trueProgress)
                    }
                }

                override fun onStateChanged(p0: View, state: Int) {
                    val canShowNav = (viewModel.getCurrentChapter()?.pages?.size ?: 1) > 1
                    if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                        sheetBehavior?.isHideable = false
                        (binding.chapterRecycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            adapter?.getPosition(viewModel.getCurrentChapter()?.chapter?.id ?: 0L) ?: 0,
                            binding.chapterRecycler.height / 2 - 30.dpToPx,
                        )
                        if (canShowNav) {
                            activity.binding.readerNav.root.isVisible = true
                        }
                        activity.binding.readerNav.root.alpha = 1f
                    }
                    if (state == BottomSheetBehavior.STATE_DRAGGING || state == BottomSheetBehavior.STATE_SETTLING) {
                        if (canShowNav) {
                            activity.binding.readerNav.root.isVisible = true
                        }
                    }
                    if (state == BottomSheetBehavior.STATE_EXPANDED) {
                        if (canShowNav) {
                            activity.binding.readerNav.root.isInvisible = true
                        }
                        activity.binding.readerNav.root.alpha = 0f
                        binding.chapterRecycler.alpha = 1f
                        if (activity.sheetManageNavColor) activity.window.navigationBarColor = navPrimary
                    }
                    if (state == BottomSheetBehavior.STATE_HIDDEN) {
                        activity.binding.readerNav.root.alpha = 0f
                        if (canShowNav) {
                            activity.binding.readerNav.root.isInvisible = true
                        }
                        binding.root.isInvisible = true
                    } else if (binding.root.isVisible) {
                        binding.root.isVisible = true
                    }
                    binding.chapterRecycler.isClickable = state == BottomSheetBehavior.STATE_EXPANDED
                    binding.chapterRecycler.isFocusable = state == BottomSheetBehavior.STATE_EXPANDED
                    activity.reEnableBackPressedCallBack()
                }
            },
        )

        adapter = FastAdapter.with(itemAdapter)
        binding.chapterRecycler.adapter = adapter
        adapter?.onClickListener = { _, _, item, position ->
            if (!sheetBehavior.isExpanded() || activity.isLoading) {
                false
            } else {
                if (item.chapter.id != viewModel.getCurrentChapter()?.chapter?.id) {
                    activity.binding.readerNav.leftChapter.isInvisible = true
                    activity.binding.readerNav.rightChapter.isInvisible = true
                    activity.isScrollingThroughPagesOrChapters = true

                    loadingPos = position
                    val itemView = (binding.chapterRecycler.findViewHolderForAdapterPosition(position) as? ReaderChapterItem.ViewHolder)?.binding
                    itemView?.bookmarkImage?.isVisible = false
                    itemView?.progress?.isVisible = true
                    activity.lifecycleScope.launch {
                        activity.loadChapter(item.chapter)
                    }
                }
                true
            }
        }
        adapter?.addEventHook(
            object : ClickEventHook<ReaderChapterItem>() {
                override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                    return if (viewHolder is ReaderChapterItem.ViewHolder) {
                        viewHolder.binding.bookmarkButton
                    } else {
                        null
                    }
                }

                override fun onClick(
                    v: View,
                    position: Int,
                    fastAdapter: FastAdapter<ReaderChapterItem>,
                    item: ReaderChapterItem,
                ) {
                    if (!activity.isLoading && sheetBehavior.isExpanded()) {
                        viewModel.toggleBookmark(item.chapter)
                        refreshList()
                    }
                }
            },
        )

        backgroundTintList = ColorStateList.valueOf(
            if (!sheetBehavior.isExpanded()) {
                primary
            } else {
                fullPrimary
            },
        )

        binding.chapterRecycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE ||
                        newState == RecyclerView.SCROLL_STATE_SETTLING
                    ) {
                        sheetBehavior?.isDraggable = true
                    } else {
                        sheetBehavior?.isDraggable = !recyclerView.canScrollVertically(-1)
                    }
                }
            },
        )

        binding.chapterRecycler.layoutManager = LinearLayoutManager(context)
        refreshList()
    }

    fun resetChapter() {
        val itemView = (binding.chapterRecycler.findViewHolderForAdapterPosition(loadingPos) as? ReaderChapterItem.ViewHolder)?.binding
        itemView?.bookmarkImage?.isVisible = true
        itemView?.progress?.isVisible = false
    }

    fun refreshList() {
        launchUI {
            val chapters = viewModel.getChapters()

            selectedChapterId = chapters.find { it.isCurrent }?.chapter?.id ?: -1L
            itemAdapter.clear()
            itemAdapter.add(chapters)

            (binding.chapterRecycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                adapter?.getPosition(viewModel.getCurrentChapter()?.chapter?.id ?: 0L) ?: 0,
                binding.chapterRecycler.height / 2 - 30.dpToPx,
            )
        }
    }

    fun lerpColor(colorStart: Int, colorEnd: Int, percent: Float): Int {
        val perc = (percent * 100).roundToInt()
        return Color.argb(
            lerpColorCalc(Color.alpha(colorStart), Color.alpha(colorEnd), perc),
            lerpColorCalc(Color.red(colorStart), Color.red(colorEnd), perc),
            lerpColorCalc(Color.green(colorStart), Color.green(colorEnd), perc),
            lerpColorCalc(Color.blue(colorStart), Color.blue(colorEnd), perc),
        )
    }

    fun lerpColorCalc(colorStart: Int, colorEnd: Int, percent: Int): Int {
        return (
            min(colorStart, colorEnd) * (100 - percent) + max(
                colorStart,
                colorEnd,
            ) * percent
            ) / 100
    }
}
