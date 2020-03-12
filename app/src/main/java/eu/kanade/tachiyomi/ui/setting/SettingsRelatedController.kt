package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.related.RelatedUpdateJob

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
                    RelatedUpdateJob.setupTask()
                    RelatedUpdateJob.runTaskNow()
                } else {
                    RelatedUpdateJob.cancelTask()
                }
            }
        }

        multiSelectListPreferenceMat(activity) {
            key = Keys.relatedUpdateRestriction
            titleRes = R.string.pref_related_update_restriction
            entriesRes = arrayOf(R.string.wifi, R.string.charging)
            entryValues = listOf("wifi", "ac")
            customSummaryRes = R.string.pref_related_update_restriction_summary
            onChange {
                RelatedUpdateJob.setupTask()
                true
            }
        }

        preference {
            title = "Credits"
            val url = "https://github.com/goldbattle/MangadexRecomendations"
            summary = context.resources.getString(R.string.pref_related_credit_message, url)
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            isIconSpaceReserved = true
        }
    }
}
