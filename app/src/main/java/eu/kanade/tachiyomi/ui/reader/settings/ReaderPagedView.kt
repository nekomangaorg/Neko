package eu.kanade.tachiyomi.ui.reader.settings

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderPagedLayoutBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView

class ReaderPagedView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderPagedLayoutBinding>(context, attrs) {

    override fun inflateBinding() = ReaderPagedLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        with(binding) {
            scaleType.bindToPreference(preferences.imageScaleType(), 1) {
                val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaReadingMode() ?: 0
                val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
                updatePagedGroup(!isWebtoonView)
            }
            zoomStart.bindToPreference(preferences.zoomStart(), 1)
            cropBorders.bindToPreference(preferences.cropBorders())
            pageTransitions.bindToPreference(preferences.pageTransitions())
            pagerNav.bindToPreference(preferences.navigationModePager())
            pagerInvert.bindToPreference(preferences.pagerNavInverted())
            extendPastCutout.bindToPreference(preferences.pagerCutoutBehavior())
            pageLayout.bindToPreference(preferences.pageLayout()) {
                val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaReadingMode() ?: 0
                val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
                updatePagedGroup(!isWebtoonView)
            }
            invertDoublePages.bindToPreference(preferences.invertDoublePages())

            pageLayout.title = pageLayout.title.toString().addBetaTag(context)

            val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaReadingMode() ?: 0
            val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
            val hasMargins = mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue
            cropBordersWebtoon.bindToPreference(if (hasMargins) preferences.cropBorders() else preferences.cropBordersWebtoon())
            webtoonSidePadding.bindToIntPreference(
                preferences.webtoonSidePadding(),
                R.array.webtoon_side_padding_values
            )
            webtoonEnableZoomOut.bindToPreference(preferences.webtoonEnableZoomOut())
            webtoonNav.bindToPreference(preferences.navigationModeWebtoon())
            webtoonInvert.bindToPreference(preferences.webtoonNavInverted())

            updatePagedGroup(!isWebtoonView)
        }
    }

    fun updatePrefs() {
        val mangaViewer = activity.presenter.getMangaReadingMode()
        val isWebtoonView = ReadingModeType.isWebtoonType(mangaViewer)
        val hasMargins = mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue
        binding.cropBordersWebtoon.bindToPreference(if (hasMargins) preferences.cropBorders() else preferences.cropBordersWebtoon())
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
            binding.pageLayout
        ).forEach { it.isVisible = show }
        listOf(
            binding.cropBordersWebtoon,
            binding.webtoonSidePadding,
            binding.webtoonEnableZoomOut,
            binding.webtoonNav,
            binding.webtoonInvert
        ).forEach { it.isVisible = !show }
        val isFullFit = when (preferences.imageScaleType().get()) {
            SubsamplingScaleImageView.SCALE_TYPE_FIT_HEIGHT,
            SubsamplingScaleImageView.SCALE_TYPE_SMART_FIT,
            SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP -> true
            else -> false
        }
        val ogView = (context as? Activity)?.window?.decorView
        val hasCutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ogView?.rootWindowInsets?.displayCutout?.safeInsetTop != null || ogView?.rootWindowInsets?.displayCutout?.safeInsetBottom != null
        } else {
            false
        }
        binding.extendPastCutout.isVisible = show && isFullFit && hasCutout
        binding.invertDoublePages.isVisible = show && preferences.pageLayout().get() != PageLayout.SINGLE_PAGE.value
    }
}
