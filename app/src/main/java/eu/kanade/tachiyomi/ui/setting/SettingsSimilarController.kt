package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.similar.SimilarUpdateJob
import eu.kanade.tachiyomi.util.system.getFilePicker
import eu.kanade.tachiyomi.util.system.toast
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

        switchPreference {
            key = Keys.similarOnlyOverWifi
            titleRes = R.string.only_download_over_wifi
            defaultValue = true
            onClick {
                SimilarUpdateJob.setupTask(true)
            }
        }

        preference {
            titleRes = R.string.similar_manually_update
            summary = context.resources.getString(R.string.similar_manually_update_message)
            onClick {
                SimilarUpdateJob.doWorkNow()
                context.toast(R.string.similar_manually_toast)
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
            titleRes = R.string.similar_from_file
            summary = context.resources.getString(R.string.similar_from_file_message)
            onClick {
                val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
                chooseFile.type = "*/*"
                val intent = Intent.createChooser(chooseFile, "Choose a file")
                try {
                    startActivityForResult(intent, RELATED_FILE_PATH_L)
                } catch (e: ActivityNotFoundException) {
                    startActivityForResult(preferences.context.getFilePicker("/"), RELATED_FILE_PATH_L)
                }
            }
        }

        preference {
            titleRes = R.string.similar_credit_title
            val url = "https://github.com/goldbattle/MangadexRecomendations"
            summary = context.resources.getString(R.string.similar_credit_message, url)
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
            }
            isIconSpaceReserved = true
        }
    }

    /**
     * This function is the callback from our file listing activity.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RELATED_FILE_PATH_L -> if (data != null && resultCode == Activity.RESULT_OK) {
                val context = applicationContext ?: return
                val selectedFile = data.data
                if (selectedFile != null) {
                    SimilarUpdateJob.doWorkNowLocal(selectedFile)
                    context.toast(R.string.similar_manually_toast)
                } else {
                    context.toast(R.string.similar_loading_complete_error)
                }
            }
        }
    }

    private companion object {
        const val RELATED_FILE_PATH_L = 105
    }
}
