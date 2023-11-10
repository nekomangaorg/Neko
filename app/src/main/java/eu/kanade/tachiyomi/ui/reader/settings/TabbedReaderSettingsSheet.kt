package eu.kanade.tachiyomi.ui.reader.settings

import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderColorFilterBinding
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog

class TabbedReaderSettingsSheet(
    val readerActivity: ReaderActivity,
    showColorFilterSettings: Boolean = false,
) : TabbedBottomSheetDialog(readerActivity) {
    private val generalView: ReaderGeneralView = View.inflate(
        readerActivity,
        R.layout.reader_general_layout,
        null,
    ) as ReaderGeneralView
    private val pagedView: ReaderPagedView = View.inflate(
        readerActivity,
        R.layout.reader_paged_layout,
        null,
    ) as ReaderPagedView
    private val filterView: ReaderFilterView = View.inflate(
        readerActivity,
        R.layout.reader_color_filter,
        null,
    ) as ReaderFilterView

    var showWebtoonView: Boolean = run {
        val mangaViewer = readerActivity.viewModel.getMangaReadingMode()
        ReadingModeType.isWebtoonType(mangaViewer)
    }

    override var offset = 0

    private val backgroundDimAnimator by lazy {
        val sheetBackgroundDim = window?.attributes?.dimAmount ?: 0.25f
        ValueAnimator.ofFloat(sheetBackgroundDim, 0f).also { valueAnimator ->
            valueAnimator.duration = 250
            valueAnimator.addUpdateListener {
                window?.setDimAmount(it.animatedValue as Float)
            }
        }
    }

    override fun getTabViews(): List<View> = listOf(
        generalView,
        pagedView,
        filterView,
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.general,
        if (showWebtoonView) R.string.webtoon else R.string.paged,
        R.string.filter,
    )

    init {
        generalView.activity = readerActivity
        pagedView.activity = readerActivity
        filterView.activity = readerActivity
        filterView.window = window
        generalView.sheet = this

        ReaderColorFilterBinding.bind(filterView).swipeDown.setOnClickListener {
            if (sheetBehavior.isCollapsed()) {
                sheetBehavior.expand()
            } else {
                sheetBehavior.collapse()
            }
        }

        binding.menu.isVisible = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            binding.menu.tooltipText = context.getString(R.string.reader_settings)
        }
        binding.menu.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_outline_settings_24dp,
            ),
        )
        binding.menu.setOnClickListener {
            val intent = SearchActivity.openReaderSettings(readerActivity)
            readerActivity.startActivity(intent)
            dismiss()
        }

        val attrs = window?.attributes
        val filterTabIndex = getTabViews().indexOf(filterView)
        binding.pager.adapter?.notifyDataSetChanged()
        binding.tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val isFilterTab = tab?.position == filterTabIndex

                    // Remove dimmed backdrop so color filter changes can be previewed
                    backgroundDimAnimator.run {
                        if (isFilterTab) {
                            if (animatedFraction < 1f) {
                                start()
                            }
                        } else if (animatedFraction > 0f) {
                            reverse()
                        }
                    }
                    readerActivity.binding.appBar.isInvisible = tab?.position == filterTabIndex
                    if (tab?.position == 2) {
                        sheetBehavior.skipCollapsed = false
                        sheetBehavior.peekHeight = 110.dpToPx
                        filterView.setWindowBrightness()
                    } else {
                        sheetBehavior.expand()
                        sheetBehavior.skipCollapsed = true
                        window?.attributes = window?.attributes?.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            },
        )

        if (showColorFilterSettings) {
            binding.tabs.getTabAt(filterTabIndex)?.select()
        }
    }

    override fun onStart() {
        super.onStart()
        val filterTabIndex = getTabViews().indexOf(filterView)
        sheetBehavior.skipCollapsed = binding.tabs.selectedTabPosition != filterTabIndex
    }

    override fun dismiss() {
        super.dismiss()
        readerActivity.binding.appBar.isVisible = true
    }

    fun updateTabs(isWebtoon: Boolean) {
        showWebtoonView = isWebtoon
        binding.pager.adapter?.notifyDataSetChanged()
        pagedView.updatePrefs()
    }
}
