package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import uy.kohesive.injekt.injectLazy

abstract class BaseReaderSettingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    internal val preferences by injectLazy<PreferencesHelper>()
    lateinit var activity: ReaderActivity

    abstract fun initGeneralPreferences()

    override fun onFinishInflate() {
        super.onFinishInflate()
        initGeneralPreferences()
    }
}