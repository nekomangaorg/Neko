package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Notification
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.util.getFilePicker
import eu.kanade.tachiyomi.util.toast
import kotlinx.io.InputStream
import org.json.JSONException
import org.json.JSONObject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.*
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import android.app.ProgressDialog
import eu.kanade.tachiyomi.data.database.models.MangaRelatedImpl


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

                // Parse the url from the specified uri
                val context = applicationContext ?: return
                val activity = activity ?: return
                val selectedFile = data.data
                val input: InputStream? = context.getContentResolver()?.openInputStream(data.data!!)

                // Check if we where able to open it
                if(input==null) {
                    context.toast("Unable to open file $selectedFile")
                    return
                }

                //=============================================================
                //=============================================================

                // Load everything from the buffer into our string
                val result = input.bufferedReader().use { it.readText() }
                val relatedPageResult: JSONObject
                try {
                    relatedPageResult = JSONObject(result)
                } catch (e : JSONException) {
                    context.toast("Unable to parse. Is it a valid JSON?")
                    return
                }

                // Count the number of mangas we need to load
                var totaMangas: Int = 0
                for(key in relatedPageResult.keys()) {
                    totaMangas++
                }

                // Create the progress bar
                val progressDialog = ProgressDialog(activity)
                progressDialog.isIndeterminate = false
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.setMax(totaMangas)
                progressDialog.setMessage("Loading Related Mangas from File")
                progressDialog.show()

                //=============================================================
                //=============================================================

                // Get our current database
                var db = Injekt.get<DatabaseHelper>()

                // Delete the old related table
                db.deleteAllRelated()

                // Now in a second thread lets insert into the database
                // This takes some time so display it all to the user
                Thread(Runnable {
                    try {

                        // Loop through each and insert into the database
                        var counter: Int = 0
                        for(key in relatedPageResult.keys()) {
                            var related = MangaRelatedImpl()
                            related.id = counter.toLong()
                            related.manga_id = key.toLong()
                            related.matched_ids = relatedPageResult.getJSONObject(key).getJSONArray("m_ids").toString()
                            related.matched_titles = relatedPageResult.getJSONObject(key).getJSONArray("m_titles").toString()
                            related.scores = relatedPageResult.getJSONObject(key).getJSONArray("scores").toString()
                            db.insertRelated(related).executeAsBlocking()
                            counter++
                            progressDialog.setProgress(counter)
                        }
                        progressDialog.dismiss()
                        context.toast("Loaded $counter mangas from file")

                        // Nice debug to see if they were inserted
                        //var mangas = db.getAllRelated().executeAsBlocking()
                        //context.toast("There are ${mangas.size} in the database")

                        // Finally, save when we last loaded the database
                        val dataFormater = SimpleDateFormat("MMM dd, yyyy HH:mm:ss zzz", Locale.getDefault())
                        val currentDate = dataFormater.format(Date())
                        preferences.relatedLastUpdated().set(context.resources.getString(R.string.pref_related_last_updated)+": "+currentDate.toString())

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }).start()


            }
        }
    }

    private companion object {
        const val RELATED_FILE_PATH_L = 105
    }

}
