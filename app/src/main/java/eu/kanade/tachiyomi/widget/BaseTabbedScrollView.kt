package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recents.RecentsController
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import uy.kohesive.injekt.injectLazy

abstract class BaseTabbedScrollView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    lateinit var binding: VB
        private set
    init {
        clipToPadding = false
    }
    internal val preferences by injectLazy<PreferencesHelper>()

    abstract fun initGeneralPreferences()
    abstract fun inflateBinding(): VB

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = inflateBinding()
        setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        initGeneralPreferences()
    }
}

abstract class BaseRecentsDisplayView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView<VB>(context, attrs) {
    var controller: RecentsController? = null
}

abstract class BaseLibraryDisplayView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView<VB>(context, attrs) {
    var controller: LibraryController? = null
}

abstract class BaseReaderSettingsView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView<VB>(context, attrs) {
    lateinit var activity: ReaderActivity
}
