package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.isLongStrip
import eu.kanade.tachiyomi.databinding.ReaderGeneralLayoutBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView

class ReaderGeneralView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderGeneralLayoutBinding>(context, attrs) {

    lateinit var sheet: TabbedReaderSettingsSheet
    override fun inflateBinding() = ReaderGeneralLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.viewerSeries.onItemSelectedListener = { position ->
            val readingModeType = ReadingModeType.fromSpinner(position)
            (context as ReaderActivity).presenter.setMangaReadingMode(readingModeType.flagValue)

            val mangaViewer = activity.presenter.getMangaReadingMode()
            if (mangaViewer == ReadingModeType.WEBTOON.flagValue || mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewerSeries.setSelection(
            (context as? ReaderActivity)?.presenter?.manga?.readingModeType?.let {
                ReadingModeType.fromPreference(it).prefValue
            } ?: 0
        )
        binding.rotationMode.onItemSelectedListener = { position ->
            val rotationType = OrientationType.fromSpinner(position)
            (context as ReaderActivity).presenter.setMangaOrientationType(rotationType.flagValue)
        }
        binding.rotationMode.setSelection(
            (context as ReaderActivity).presenter.manga?.orientationType?.let {
                OrientationType.fromPreference(it).prefValue
            } ?: 0
        )

        binding.backgroundColor.bindToPreference(preferences.readerTheme(), 0)
        binding.showPageNumber.bindToPreference(preferences.showPageNumber())
        binding.fullscreen.bindToPreference(preferences.fullscreen())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.alwaysShowChapterTransition.bindToPreference(preferences.alwaysShowChapterTransition())
    }

    fun checkIfShouldDisableReadingMode() {
        if (activity.presenter.manga?.isLongStrip() == true) {
            binding.viewerSeries.setDisabledState(R.string.webtoon_cannot_change)
        }
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        sheet.updateTabs(true)
    }

    private fun initPagerPreferences() {
        sheet.updateTabs(false)
    }
}
