package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.related.RelatedJob

class SettingsRelatedController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_related

        preference {
            titleRes = R.string.pref_related_info_tab
            summary = context.resources.getString(R.string.pref_related_summary_message)
            isIconSpaceReserved = true
        }

        switchPreference {
            key = Keys.relatedShowTab
            titleRes = R.string.pref_related_show_tab
            defaultValue = false
            onClick {
                if (isChecked) {
                    RelatedJob.setupTask()
                    RelatedJob.runTaskNow()
                } else {
                    RelatedJob.cancelTask()
                }
            }
        }
    }
}
