package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.similar.SimilarUpdateJob

class SettingsSimilarController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_similar

        preference {
            titleRes = R.string.pref_similar_info_tab
            summary = context.resources.getString(R.string.pref_similar_summary_message)
            isIconSpaceReserved = true
        }

        switchPreference {
            key = Keys.similarEnabled
            titleRes = R.string.pref_similar_show_tab
            defaultValue = false
            onClick {
                if (isChecked) {
                    SimilarUpdateJob.setupTask()
                    SimilarUpdateJob.runTaskNow()
                } else {
                    SimilarUpdateJob.cancelTask()
                }
            }
        }

        multiSelectListPreferenceMat(activity) {
            key = Keys.similarUpdateRestriction
            titleRes = R.string.pref_similar_update_restriction
            entriesRes = arrayOf(R.string.wifi, R.string.charging)
            entryValues = listOf("wifi", "ac")
            customSummaryRes = R.string.pref_similar_update_restriction_summary
            onChange {
                SimilarUpdateJob.cancelTask()
                SimilarUpdateJob.setupTask()
                true
            }
        }

        preference {
            title = "Credits"
            val url = "https://github.com/goldbattle/MangadexRecomendations"
            summary = context.resources.getString(R.string.pref_similar_credit_message, url)
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            isIconSpaceReserved = true
        }
    }
}
