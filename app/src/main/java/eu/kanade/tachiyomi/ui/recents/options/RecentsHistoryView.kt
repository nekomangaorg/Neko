package eu.kanade.tachiyomi.ui.recents.options

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.databinding.RecentsHistoryViewBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseRecentsDisplayView

class RecentsHistoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseRecentsDisplayView<RecentsHistoryViewBinding>(context, attrs) {

    override fun inflateBinding() = RecentsHistoryViewBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.groupChapters.bindToPreference(preferences.groupChaptersHistory())
    }
}
