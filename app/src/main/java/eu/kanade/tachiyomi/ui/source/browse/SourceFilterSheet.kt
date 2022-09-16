package eu.kanade.tachiyomi.ui.source.browse

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.databinding.SourceFilterSheetBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.checkHeightThen
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.widget.E2EBottomSheetDialog

class SourceFilterSheet(val activity: Activity) :
    E2EBottomSheetDialog<SourceFilterSheetBinding>(activity) {

    private var filterChanged = true

    val adapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter<IFlexible<*>>(null)
        .setDisplayHeadersAtStartUp(true)

    var onSearchClicked = {}

    var onResetClicked = {}

    var onRandomClicked = {}

    var onFollowsClicked = {}

    var onLatestChapterClicked = {}

    override var recyclerView: RecyclerView? = binding.filtersRecycler

    override fun createBinding(inflater: LayoutInflater) =
        SourceFilterSheetBinding.inflate(inflater)

    init {
        binding.searchBtn.setOnClickListener { dismiss() }
        binding.resetBtn.setOnClickListener { onResetClicked() }
        binding.randomMangaBtn.setOnClickListener { onRandomClicked() }
        binding.followsBtn.setOnClickListener { onFollowsClicked() }
        binding.latestChaptersBtn.setOnClickListener { onLatestChapterClicked() }

        binding.titleLayout.checkHeightThen {
            activity.window.decorView.rootWindowInsetsCompat?.let { setCardViewMax(it) }
        }

        binding.cardView.doOnApplyWindowInsetsCompat { _, insets, _ ->
            binding.cardView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                val fullHeight = activity.window.decorView.height
                matchConstraintMaxHeight =
                    fullHeight - insets.getInsets(systemBars()).top -
                    binding.titleLayout.height - 75.dpToPx
            }
        }

        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = context.obtainStyledAttributes(attrsArray)
        val headerHeight = array.getDimensionPixelSize(0, 0)
        array.recycle()
        binding.root.doOnApplyWindowInsetsCompat { _, insets, _ ->
            binding.titleLayout.updatePaddingRelative(
                bottom = insets.getInsets(systemBars()).bottom,
            )
            binding.titleLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = headerHeight + headerHeight + binding.titleLayout.paddingBottom
            }
            setCardViewMax(insets)
        }

        (binding.root.parent.parent as? View)?.viewTreeObserver?.addOnGlobalLayoutListener(
            object :
                OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    updateBottomButtons()
                    if (sheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                        (binding.root.parent.parent as? View)?.viewTreeObserver?.removeOnGlobalLayoutListener(
                            this,
                        )
                    }
                }
            },
        )

        binding.filtersRecycler.viewTreeObserver.addOnScrollChangedListener {
            updateBottomButtons()
        }

        setOnShowListener {
            updateBottomButtons()
        }

        binding.filtersRecycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.filtersRecycler.clipToPadding = false
        binding.filtersRecycler.adapter = adapter
        binding.filtersRecycler.setHasFixedSize(false)

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    updateBottomButtons()
                }

                override fun onStateChanged(p0: View, state: Int) {
                    updateBottomButtons()
                }
            },
        )
    }

    fun setCardViewMax(insets: WindowInsetsCompat) {
        val fullHeight = activity.window.decorView.height
        val newHeight = fullHeight - insets.getInsets(systemBars()).top -
            binding.titleLayout.height - 75.dpToPx
        if ((binding.cardView.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxHeight != newHeight) {
            binding.cardView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintMaxHeight = newHeight
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.expand()
        sheetBehavior.skipCollapsed = true
        updateBottomButtons()
        binding.root.post {
            updateBottomButtons()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = context.obtainStyledAttributes(attrsArray)
        val headerHeight = array.getDimensionPixelSize(0, 0)
        binding.titleLayout.updatePaddingRelative(
            bottom = activity.window.decorView.rootWindowInsetsCompat
                ?.getInsets(systemBars())?.bottom ?: 0,
        )

        binding.titleLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = headerHeight + headerHeight + binding.titleLayout.paddingBottom
        }
        array.recycle()
    }

    private fun updateBottomButtons() {
        val bottomSheet = binding.root.parent as View
        val bottomSheetVisibleHeight =
            -bottomSheet.top + (activity.window.decorView.height - bottomSheet.height)

        binding.titleLayout.translationY = bottomSheetVisibleHeight.toFloat()
    }

    override fun dismiss() {
        super.dismiss()
        if (filterChanged) {
            onSearchClicked()
        }
    }

    fun setFilters(items: List<IFlexible<*>>) {
        adapter.updateDataSet(items)
    }
}
