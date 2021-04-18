package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
import android.view.ViewGroup
import androidx.core.text.set
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.RecentsOptionsSheetBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import uy.kohesive.injekt.injectLazy

class RecentsOptionsSheet(activity: Activity) :
    BottomSheetDialog(activity, R.style.BottomSheetDialogTheme) {

    private val binding = RecentsOptionsSheetBinding.inflate(activity.layoutInflater)
    private val preferences by injectLazy<PreferencesHelper>()

    init {
        setContentView(binding.root)
        setEdgeToEdge(activity, binding.root)
        initGeneralPreferences()
        binding.settingsScrollView.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(binding.root.parent as ViewGroup).skipCollapsed = true

        val titleText = context.getString(R.string.show_reset_history_button)
        val subtitleText = context.getString(R.string.press_and_hold_to_also_reset)
        binding.showRemoveHistory.text = titleText.withSubtitle(context, subtitleText)
    }

    private fun initGeneralPreferences() {
        binding.showRecentsDownload.bindToPreference(preferences.showRecentsDownloads())
        binding.showRemoveHistory.bindToPreference(preferences.showRecentsRemHistory())
        binding.showReadInAll.bindToPreference(preferences.showReadInAllRecents())
        binding.showTitleFirst.bindToPreference(preferences.showTitleFirstInRecents())
    }
}
