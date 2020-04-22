package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.similar.SimilarUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsSimilarController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.similar_settings

        preference {
            titleRes = R.string.similar_screen
            summary = context.resources.getString(R.string.similar_screen_summary_message)
            isIconSpaceReserved = true
        }

        switchPreference {
            key = Keys.similarEnabled
            titleRes = R.string.similar_screen
            defaultValue = false
            onClick {
                SimilarUpdateJob.setupTask()
            }
        }

        multiSelectListPreferenceMat(activity)
        {
            key = Keys.similarUpdateRestriction
            titleRes = R.string.similar_update_restriction
            entriesRes = arrayOf(R.string.wifi, R.string.charging)
            entryValues = listOf("wifi", "ac")
            customSummaryRes = R.string.similar_update_restriction_summary
            onChange {
                SimilarUpdateJob.setupTask()
                true
            }
        }

        preference {
            title = "Credits"
            val url = "https://github.com/goldbattle/MangadexRecomendations"
            summary = context.resources.getString(R.string.similar_credit_message, url)
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            isIconSpaceReserved = true
        }
    }
}
