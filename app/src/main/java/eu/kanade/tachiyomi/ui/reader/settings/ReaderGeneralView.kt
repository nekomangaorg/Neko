package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import org.nekomanga.databinding.ReaderGeneralLayoutBinding

class ReaderGeneralView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderGeneralLayoutBinding>(context, attrs) {

    lateinit var sheet: TabbedReaderSettingsSheet

    override fun inflateBinding() = ReaderGeneralLayoutBinding.bind(this)

    override fun initGeneralPreferences() {
        binding.viewerSeries.onItemSelectedListener = { position ->
            val readingModeType = ReadingModeType.fromSpinner(position)
            (context as ReaderActivity).viewModel.setMangaReadingMode(readingModeType.flagValue)

            when (activity.viewModel.getMangaReadingMode() == ReadingModeType.WEBTOON.flagValue) {
                true -> initWebtoonPreferences()
                else -> initPagerPreferences()
            }
        }
        binding.viewerSeries.setSelection(
            (context as? ReaderActivity)?.viewModel?.state?.value?.manga?.readingModeType?.let {
                ReadingModeType.fromPreference(it).prefValue
            } ?: 0
        )
        binding.rotationMode.onItemSelectedListener = { position ->
            val rotationType = OrientationType.fromSpinner(position)
            (context as ReaderActivity).viewModel.setMangaOrientationType(rotationType.flagValue)
        }
        binding.rotationMode.setSelection(
            (context as ReaderActivity).viewModel.manga?.orientationType?.let {
                OrientationType.fromPreference(it).prefValue
            } ?: 0
        )

        binding.backgroundColor.bindToPreference(readerPreferences.readerTheme(), 0)
        binding.showPageNumber.bindToPreference(readerPreferences.showPageNumber())
        binding.fullscreen.bindToPreference(readerPreferences.fullscreen())
        binding.keepscreen.bindToPreference(readerPreferences.keepScreenOn())
        binding.alwaysShowChapterTransition.bindToPreference(
            readerPreferences.alwaysShowChapterTransition()
        )
    }

    /** Init the preferences for the webtoon reader. */
    private fun initWebtoonPreferences() {
        sheet.updateTabs(true)
    }

    private fun initPagerPreferences() {
        sheet.updateTabs(false)
    }
}
