package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.getFilePicker
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import com.davemorrissey.labs.subscaleview.ImageSource.uri
import android.provider.MediaStore
import android.content.ContentUris
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION
import android.os.Build.VERSION.SDK_INT
import android.content.Context
import android.database.Cursor
import android.os.Build


class SettingsRelatedController : SettingsController() {

    private val db: DatabaseHelper by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_related


        switchPreference {
            key = Keys.relatedShowTab
            titleRes = R.string.pref_related_show_tab
            defaultValue = false
        }

        preference {
            key = Keys.relatedFilePath
            titleRes = R.string.pref_related_file_path
            onClick {
                val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
                chooseFile.setType("*/*")
                val intent = Intent.createChooser(chooseFile, "Choose a file")
                try {
                    startActivityForResult(intent, RELATED_FILE_PATH_L)
                } catch (e: ActivityNotFoundException) {
                    startActivityForResult(preferences.context.getFilePicker("/"), RELATED_FILE_PATH_L)
                }
            }

            preferences.relatedFilePath().asObservable()
                    .subscribeUntilDestroy { path ->
                        summary = path
                    }
        }

        preference {
            title = "Github"
            val url = "https://github.com/goldbattle/MangadexRecomendations"
            summary = url
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            isIconSpaceReserved = true
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RELATED_FILE_PATH_L -> if (data != null && resultCode == Activity.RESULT_OK) {
                // The file url we have isn't in the right format, so extract the parts
                val docId = DocumentsContract.getDocumentId(data.data)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // External storage is in XXXX-XXXX:path/on/storage.json
                // Otherwise, it is just a raw local file
                if (!"raw".equals(split[0], ignoreCase=true)) {
                    preferences.relatedFilePath().set("/storage/"+split[0]+"/"+split[1])
                } else {
                    preferences.relatedFilePath().set(split[1])
                }

            }
        }
    }

    private companion object {
        const val RELATED_FILE_PATH_L = 105
    }
}
