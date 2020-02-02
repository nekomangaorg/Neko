package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MangaRelatedImpl
import eu.kanade.tachiyomi.util.getFilePicker
import eu.kanade.tachiyomi.util.toast
import org.json.JSONObject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys


class SettingsRelatedController : SettingsController() {

    private val db: DatabaseHelper by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_related



        preference {
            title = "Information"
            summary = "This is an alpha feature where one can get manga recommendations. " +
                    "This is a recommendation system outside of MangaDex, and works by " +
                    "matching by genres, demographics, and then using term frequency–inverse " +
                    "document frequency (Tfidf) to get the similarity of two manga's " +
                    "descriptions. You will need to download from the below github the " +
                    "recommendation JSON manually."
            onClick {
            }
            isIconSpaceReserved = true
        }

        switchPreference {
            key = Keys.relatedShowTab
            titleRes = R.string.pref_related_show_tab
            defaultValue = false
        }

        preference {
            key = Keys.relatedLastUpdated
            titleRes = R.string.pref_related_last_updated_txt
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

            preferences.relatedLastUpdated().asObservable()
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

                //=============================================================
                //=============================================================

                // The file url we have isn't in the right format, so extract the parts
                //val docId = DocumentsContract.getDocumentId(data.data)
                var docstr = data.data!!.getPath()
                val split = docstr!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // Assume that if we couldn't split, it is on an external drive
                if (split.size < 2) return
                // External storage is in XXXX-XXXX:path/on/storage.json
                // Otherwise, it is just a raw local file
                //if (!"raw".equals(split[0], ignoreCase=true) && !"file".equals(split[0], ignoreCase=true)) {
                //    preferences.relatedLastUpdated().set(Environment.getExternalStorageDirectory().toString()+"/"+split[1])
                //} else {
                //    preferences.relatedLastUpdated().set(split[1])
                //}
                val filePath = split[1]

                //=============================================================
                //=============================================================

                // Get our current database
                var db = Injekt.get<DatabaseHelper>()

                // Delete the old related table
                db.deleteAllRelated()

                // Open the manga file
                var result = File(filePath).readText(Charsets.UTF_8)
                val relatedPageResult = JSONObject(result)

                // Loop through each and insert into the database
                var counter: Long = 0
                for(key in relatedPageResult.keys()) {
                    var related = MangaRelatedImpl()
                    related.id = counter
                    related.manga_id = key.toLong()
                    related.matched_ids = relatedPageResult.getJSONObject(key).getJSONArray("m_ids").toString()
                    related.matched_titles = relatedPageResult.getJSONObject(key).getJSONArray("m_titles").toString()
                    related.scores = relatedPageResult.getJSONObject(key).getJSONArray("scores").toString()
                    db.insertRelated(related).executeAsBlocking()
                    counter++
                }

                // Lets now open our manga file
                val context = applicationContext ?: return
                context.toast("Loaded $counter mangas from file")

                // Nice debug to see if they were inserted
                var mangas = db.getAllRelated().executeAsBlocking()
                context.toast("There are ${mangas.size} in the database")


                // Finally, save when we last loaded the database
                val dataFormater = SimpleDateFormat("MMM dd, yyyy HH:mm:ss zzz", Locale.getDefault())
                val currentDate = dataFormater.format(Date())
                preferences.relatedLastUpdated().set(context.resources.getString(R.string.pref_related_last_updated)+": "+currentDate.toString())

            }
        }
    }

    private companion object {
        const val RELATED_FILE_PATH_L = 105
    }

}
