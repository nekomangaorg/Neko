package eu.kanade.tachiyomi.ui.reader.settings

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.view.visibleIf
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import kotlinx.android.synthetic.main.reader_paged_layout.view.*

class ReaderPagedView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView(context, attrs) {

    override fun initGeneralPreferences() {
        scale_type.bindToPreference(preferences.imageScaleType(), 1) {
            val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaViewer() ?: 0
            val isWebtoonView = mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
            updatePagedGroup(!isWebtoonView)
        }
        zoom_start.bindToPreference(preferences.zoomStart(), 1)
        crop_borders.bindToPreference(preferences.cropBorders())
        page_transitions.bindToPreference(preferences.pageTransitions())
        pager_nav.bindToPreference(preferences.navigationModePager())
        pager_invert.bindToPreference(preferences.pagerNavInverted())
        extend_past_cutout.bindToPreference(preferences.pagerCutoutBehavior())

        val mangaViewer = (context as? ReaderActivity)?.presenter?.getMangaViewer() ?: 0
        val isWebtoonView = mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
        val hasMargins = mangaViewer == ReaderActivity.VERTICAL_PLUS
        crop_borders_webtoon.bindToPreference(if (hasMargins) preferences.cropBorders() else preferences.cropBordersWebtoon())
        webtoon_side_padding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)
        webtoon_enable_zoom_out.bindToPreference(preferences.webtoonEnableZoomOut())
        webtoon_nav.bindToPreference(preferences.navigationModeWebtoon())
        webtoon_invert.bindToPreference(preferences.webtoonNavInverted())

        updatePagedGroup(!isWebtoonView)
    }

    fun updatePrefs() {
        val mangaViewer = activity.presenter.getMangaViewer()
        val isWebtoonView = mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
        val hasMargins = mangaViewer == ReaderActivity.VERTICAL_PLUS
        crop_borders_webtoon.bindToPreference(if (hasMargins) preferences.cropBorders() else preferences.cropBordersWebtoon())
        updatePagedGroup(!isWebtoonView)
    }

    private fun updatePagedGroup(show: Boolean) {
        listOf(scale_type, zoom_start, crop_borders, page_transitions, pager_nav, pager_invert).forEach { it.visibleIf(show) }
        listOf(crop_borders_webtoon, webtoon_side_padding, webtoon_enable_zoom_out, webtoon_nav, webtoon_invert).forEach { it.visibleIf(!show) }
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
        extend_past_cutout.visibleIf(show && isFullFit && hasCutout)
    }
}
