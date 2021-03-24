package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.android.synthetic.main.reader_general_layout.view.*

class ReaderGeneralView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView(context, attrs) {

    lateinit var sheet: TabbedReaderSettingsSheet
    override fun initGeneralPreferences() {
        settings_scroll_view.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        viewer_series.onItemSelectedListener = { position ->
            activity.presenter.setMangaViewer(position)

            val mangaViewer = activity.presenter.getMangaViewer()
            if (mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        viewer_series.setSelection((context as? ReaderActivity)?.presenter?.manga?.viewer ?: 0)
        rotation_mode.bindToPreference(preferences.rotation(), 1)
        background_color.bindToPreference(preferences.readerTheme(), 0)
        show_page_number.bindToPreference(preferences.showPageNumber())
        fullscreen.bindToPreference(preferences.fullscreen())
        keepscreen.bindToPreference(preferences.keepScreenOn())
        always_show_chapter_transition.bindToPreference(preferences.alwaysShowChapterTransition())
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