package eu.kanade.tachiyomi.ui.reader.settings

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import org.nekomanga.R
import org.nekomanga.databinding.ReaderPagedLayoutBinding

class ReaderPagedView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderPagedLayoutBinding>(context, attrs) {

    override fun inflateBinding() = ReaderPagedLayoutBinding.bind(this)

    override fun initGeneralPreferences() {
        with(binding) {
            scaleType.bindToPreference(readerPreferences.imageScaleType(), 1) {
                val mangaViewer =
                    (context as? ReaderActivity)?.viewModel?.getMangaReadingMode() ?: 0
                val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
                updatePagedGroup(!isWebtoonView)
                landscapeZoom.isVisible =
                    it == SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE - 1
            }

            doublePageGap.bindToIntPreference(
                readerPreferences.doublePageGap(), R.array.double_page_gap)
            binding.navigatePan.bindToPreference(readerPreferences.navigateToPan())
            binding.landscapeZoom.bindToPreference(readerPreferences.landscapeZoom())
            zoomStart.bindToPreference(readerPreferences.zoomStart(), 1)
            cropBorders.bindToPreference(readerPreferences.cropBorders())
            pageTransitions.bindToPreference(readerPreferences.animatedPageTransitions())
            pagerNav.bindToPreference(readerPreferences.navigationModePager())
            pagerInvert.bindToPreference(readerPreferences.pagerNavInverted())
            extendPastCutout.bindToPreference(readerPreferences.pagerCutoutBehavior())
            pageLayout.bindToPreference(readerPreferences.pageLayout()) {
                val mangaViewer =
                    (context as? ReaderActivity)?.viewModel?.getMangaReadingMode() ?: 0
                val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
                updatePagedGroup(!isWebtoonView)
            }

            invertDoublePages.bindToPreference(readerPreferences.invertDoublePages())

            doublePageRotateToFit.bindToPreference(readerPreferences.doublePageRotate()) {
                doublePageRotateEnabled ->
                when (doublePageRotateEnabled) {
                    true -> doublePageRotateToFitInvert.visibility = VISIBLE
                    false -> doublePageRotateToFitInvert.visibility = GONE
                }
            }
            doublePageRotateToFitInvert.bindToPreference(
                readerPreferences.doublePageRotateReverse())

            pageLayout.title =
                pageLayout.title.toString().addBetaTag(context, R.attr.colorSecondary)

            val mangaViewer = (context as? ReaderActivity)?.viewModel?.getMangaReadingMode() ?: 0
            val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
            val hasMargins = mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue
            cropBordersWebtoon.bindToPreference(
                if (hasMargins) readerPreferences.cropBorders()
                else readerPreferences.cropBordersWebtoon())
            webtoonSidePadding.bindToIntPreference(
                readerPreferences.webtoonSidePadding(),
                R.array.webtoon_side_padding_values,
            )
            webtoonEnableZoomOut.bindToPreference(readerPreferences.webtoonEnableZoomOut())
            webtoonNav.bindToPreference(readerPreferences.navigationModeWebtoon())
            webtoonInvert.bindToPreference(readerPreferences.webtoonNavInverted())
            webtoonPageLayout.bindToPreference(readerPreferences.webtoonPageLayout())
            webtoonInvertDoublePages.bindToPreference(readerPreferences.webtoonInvertDoublePages())
            webtoonPageTransitions.bindToPreference(
                readerPreferences.animatedPageTransitionsWebtoon())

            updatePagedGroup(!isWebtoonView)
        }
    }

    fun updatePrefs() {
        val mangaViewer = activity.viewModel.getMangaReadingMode()
        val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
        val hasMargins = mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue
        binding.cropBordersWebtoon.bindToPreference(
            if (hasMargins) readerPreferences.cropBorders()
            else readerPreferences.cropBordersWebtoon())
        updatePagedGroup(!isWebtoonView)
    }

    private fun updatePagedGroup(show: Boolean) {
        listOf(
                binding.scaleType,
                binding.zoomStart,
                binding.cropBorders,
                binding.pageTransitions,
                binding.pagerNav,
                binding.pagerInvert,
                binding.pageLayout,
                binding.landscapeZoom,
                binding.navigatePan,
            )
            .forEach { it.isVisible = show }
        listOf(
                binding.cropBordersWebtoon,
                binding.webtoonSidePadding,
                binding.webtoonEnableZoomOut,
                binding.webtoonNav,
                binding.webtoonInvert,
                binding.webtoonPageLayout,
                binding.webtoonInvertDoublePages,
                binding.webtoonPageTransitions,
            )
            .forEach { it.isVisible = !show }
        val isFullFit =
            when (readerPreferences.imageScaleType().get()) {
                SubsamplingScaleImageView.SCALE_TYPE_FIT_HEIGHT,
                SubsamplingScaleImageView.SCALE_TYPE_SMART_FIT,
                SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP, -> true
                else -> false
            }
        val ogView = (context as? Activity)?.window?.decorView
        val hasCutout =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ogView?.rootWindowInsets?.displayCutout?.safeInsetTop != null ||
                    ogView?.rootWindowInsets?.displayCutout?.safeInsetBottom != null
            } else {
                false
            }
        binding.landscapeZoom.isVisible =
            show &&
                readerPreferences.imageScaleType().get() ==
                    SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
        binding.extendPastCutout.isVisible =
            show && isFullFit && hasCutout && readerPreferences.fullscreen().get()
        binding.invertDoublePages.isVisible =
            show && readerPreferences.pageLayout().get() != PageLayout.SINGLE_PAGE.value
        binding.doublePageGap.isVisible =
            show && readerPreferences.pageLayout().get() != PageLayout.SINGLE_PAGE.value
    }
}
