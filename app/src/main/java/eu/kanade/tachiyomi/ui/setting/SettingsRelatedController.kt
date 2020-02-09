package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import java.text.SimpleDateFormat
import java.util.*
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.database.models.MangaRelatedImpl
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlin.system.exitProcess


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
                    context.toast(context.resources.getString(R.string.pref_related_file_error,selectedFile))
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
                    context.toast(context.resources.getString(R.string.pref_related_broken_file))
                    return
                }

                // Count the number of mangas we need to load
                var totaMangas: Int = 0
                for(key in relatedPageResult.keys()) {
                    totaMangas++
                }
                context.toast(context.resources.getString(R.string.pref_related_notify_start,totaMangas))

                //=============================================================
                //=============================================================

                // Now in a second thread lets insert into the database
                // This takes some time so display it all to the user
                Thread(Runnable {

                    // Get our current database
                    val db = Injekt.get<DatabaseHelper>()

                    // Delete the old related table
                    db.deleteAllRelated().executeAsBlocking()

                    // Build the notification which we will display the progress bar in
                    val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_MANGA_RELATED).apply {
                        setContentTitle(context.resources.getString(R.string.pref_related_loading_welcome))
                        setSmallIcon(R.drawable.ic_neko_notification)
                        setPriority(NotificationCompat.PRIORITY_LOW)
                        setOngoing(true)
                        setOnlyAlertOnce(true)
                        setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    }


                    // Issue the initial notification with zero progress
                    builder.setProgress(totaMangas, 0, false)
                    NotificationManagerCompat.from(context).notify(Notifications.ID_MANGA_RELATED_IMPORT, builder.build())

                    try {

                        // Loop through each and insert into the database
                        var counter: Int = 0
                        for(key in relatedPageResult.keys()) {

                            // check if activity is still running
                            // if it isn't then we should stop updating and delete the notification
                            if(activity.isDestroyed) {
                                NotificationManagerCompat.from(context).cancel(Notifications.ID_MANGA_RELATED_IMPORT)
                                exitProcess(0)
                            }

                            // create the implementation and insert
                            var related = MangaRelatedImpl()
                            related.id = counter.toLong()
                            related.manga_id = key.toLong()
                            related.matched_ids = relatedPageResult.getJSONObject(key).getJSONArray("m_ids").toString()
                            related.matched_titles = relatedPageResult.getJSONObject(key).getJSONArray("m_titles").toString()
                            related.scores = relatedPageResult.getJSONObject(key).getJSONArray("scores").toString()
                            db.insertRelated(related).executeAsBlocking()

                            // display to the user
                            counter++
                            builder.setProgress(totaMangas, counter, false)
                            builder.setContentTitle(context.resources.getString(R.string.pref_related_loading_percent,counter,totaMangas))
                            NotificationManagerCompat.from(context).notify(Notifications.ID_MANGA_RELATED_IMPORT, builder.build())

                        }

                        // Finally, save when we last loaded the database
                        val dataFormater = SimpleDateFormat("MMM dd, yyyy HH:mm:ss zzz", Locale.getDefault())
                        val currentDate = dataFormater.format(Date())
                        preferences.relatedLastUpdated().set(context.resources.getString(R.string.pref_related_last_updated,currentDate.toString()))

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                    // Cancel the progress bar
                    NotificationManagerCompat.from(context).cancel(Notifications.ID_MANGA_RELATED_IMPORT)

                    // Show the finished notification
                    val builder_done = NotificationCompat.Builder(context, Notifications.CHANNEL_MANGA_RELATED).apply {
                        setContentTitle(context.resources.getString(R.string.pref_related_loading_complete,totaMangas))
                        setSmallIcon(R.drawable.ic_neko_notification)
                        setPriority(NotificationCompat.PRIORITY_LOW)
                        setOnlyAlertOnce(true)
                        setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    }
                    NotificationManagerCompat.from(context).notify(Notifications.ID_MANGA_RELATED_IMPORT,builder_done.build())

                }).start()


            }
        }
    }

    private companion object {
        const val RELATED_FILE_PATH_L = 105
    }

}
