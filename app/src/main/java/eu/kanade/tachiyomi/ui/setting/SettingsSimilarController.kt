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
            defaultValue = true
            onClick {
                SimilarUpdateJob.setupTask()
            }
        }

        switchPreference {
            key = Keys.similarOnlyOverWifi
            titleRes = R.string.only_download_over_wifi
            defaultValue = true
            onClick {
                SimilarUpdateJob.setupTask()
            }
        }

        intListPreference(activity) {
            key = Keys.similarUpdateInterval
            titleRes = R.string.similar_update_fequency
            entriesRes = arrayOf(
                    R.string.manual,
                    R.string.daily,
                    R.string.every_2_days,
                    R.string.every_3_days,
                    R.string.weekly,
                    R.string.monthly
            )
            entryValues = listOf(0, 1, 2, 3, 7, 30)
            defaultValue = 3

            onChange {
                SimilarUpdateJob.setupTask(true)
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
