package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import uy.kohesive.injekt.injectLazy

abstract class BaseTabbedScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    init {
        clipToPadding = false
    }
    internal val preferences by injectLazy<PreferencesHelper>()

    abstract fun initGeneralPreferences()

    override fun onFinishInflate() {
        super.onFinishInflate()
        setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        initGeneralPreferences()
    }
}

abstract class BaseLibraryDisplayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView(context, attrs) {
    lateinit var controller: LibraryController
}

abstract class BaseReaderSettingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView(context, attrs) {
    lateinit var activity: ReaderActivity
}