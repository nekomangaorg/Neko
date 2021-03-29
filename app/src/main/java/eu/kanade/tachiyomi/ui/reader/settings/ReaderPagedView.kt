package eu.kanade.tachiyomi.ui.reader.settings

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderPagedLayoutBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.view.visibleIf
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView

class ReaderPagedView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderPagedLayoutBinding>(context, attrs) {

    override fun inflateBinding() = ReaderPagedLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.scaleType.bindToPreference(preferences.imageScaleType(), 1) {
            val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaViewer() ?: 0
            val isWebtoonView = mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
            updatePagedGroup(!isWebtoonView)
        }
        binding.zoomStart.bindToPreference(preferences.zoomStart(), 1)
        binding.cropBorders.bindToPreference(preferences.cropBorders())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())
        binding.pagerNav.bindToPreference(preferences.navigationModePager())
        binding.pagerInvert.bindToPreference(preferences.pagerNavInverted())
        binding.extendPastCutout.bindToPreference(preferences.pagerCutoutBehavior())

        val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaViewer() ?: 0
        val isWebtoonView = mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
        val hasMargins = mangaViewer == ReaderActivity.VERTICAL_PLUS
        binding.cropBordersWebtoon.bindToPreference(if (hasMargins) preferences.cropBorders() else preferences.cropBordersWebtoon())
        binding.webtoonSidePadding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)
        binding.webtoonEnableZoomOut.bindToPreference(preferences.webtoonEnableZoomOut())
        binding.webtoonNav.bindToPreference(preferences.navigationModeWebtoon())
        binding.webtoonInvert.bindToPreference(preferences.webtoonNavInverted())

        updatePagedGroup(!isWebtoonView)
    }

    fun updatePrefs() {
        val mangaViewer = activity.presenter.getMangaViewer()
        val isWebtoonView = mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
        val hasMargins = mangaViewer == ReaderActivity.VERTICAL_PLUS
        binding.cropBordersWebtoon.bindToPreference(if (hasMargins) preferences.cropBorders() else preferences.cropBordersWebtoon())
        updatePagedGroup(!isWebtoonView)
    }

    private fun updatePagedGroup(show: Boolean) {
        listOf(binding.scaleType, binding.zoomStart, binding.cropBorders, binding.pageTransitions, binding.pagerNav, binding.pagerInvert).forEach { it.visibleIf(show) }
        listOf(binding.cropBordersWebtoon, binding.webtoonSidePadding, binding.webtoonEnableZoomOut, binding.webtoonNav, binding.webtoonInvert).forEach { it.visibleIf(!show) }
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
        binding.extendPastCutout.visibleIf(show && isFullFit && hasCutout)
    }
}
