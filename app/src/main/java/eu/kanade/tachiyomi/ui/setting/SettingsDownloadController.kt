package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlowIn
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withOriginalWidth
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsDownloadController : SettingsController() {

    private val db: DatabaseHelper by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.downloads

        preference {
            key = Keys.downloadsDirectory
            titleRes = R.string.download_location
            onClick {
                DownloadDirectoriesDialog(this@SettingsDownloadController).show()
            }

            preferences.downloadsDirectory().asImmediateFlowIn(viewScope) { path ->
                val dir = UniFile.fromUri(context, path.toUri())
                summary = dir.filePath ?: path
            }
        }
        switchPreference {
            key = Keys.downloadOnlyOverWifi
            titleRes = R.string.only_download_over_wifi
            defaultValue = true
        }
        switchPreference {
            key = Keys.saveChaptersAsCBZ
            titleRes = R.string.save_chapters_as_cbz
            defaultValue = false
        }
        preferenceCategory {
            titleRes = R.string.remove_after_read

            switchPreference {
                key = Keys.removeAfterMarkedAsRead
                titleRes = R.string.remove_when_marked_as_read
                defaultValue = false
            }
            intListPreference(activity) {
                key = Keys.removeAfterReadSlots
                titleRes = R.string.remove_after_read
                entriesRes = arrayOf(
                    R.string.never,
                    R.string.last_read_chapter,
                    R.string.second_to_last,
                    R.string.third_to_last,
                    R.string.fourth_to_last,
                    R.string.fifth_to_last
                )
                entryRange = -1..4
                defaultValue = -1
            }
        }

        val dbCategories = db.getCategories().executeAsBlocking()
        val categories = listOf(Category.createDefault(context)) + dbCategories

        preferenceCategory {
            titleRes = R.string.download_new_chapters

            switchPreference {
                key = Keys.downloadNew
                titleRes = R.string.download_new_chapters
                defaultValue = false
            }
            triStateListPreference(activity) {
                key = Keys.downloadNewCategories
                excludeKey = Keys.downloadNewCategoriesExclude
                titleRes = R.string.categories
                entries = categories.map { it.name }
                entryValues = categories.map { it.id.toString() }
                allSelectionRes = R.string.all

                preferences.downloadNew().asImmediateFlowIn(viewScope) { isVisible = it }
            }
            preferenceCategory {
                titleRes = R.string.automatic_removal

                intListPreference(activity) {
                    key = Keys.deleteRemovedChapters
                    titleRes = R.string.delete_removed_chapters
                    summary = activity?.getString(R.string.delete_downloaded_if_removed_online)
                    entriesRes = arrayOf(
                        R.string.ask_on_chapters_page,
                        R.string.always_keep,
                        R.string.always_delete
                    )
                    entryRange = 0..2
                    defaultValue = 0
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DOWNLOAD_DIR -> if (data != null && resultCode == Activity.RESULT_OK) {
                val context = applicationContext ?: return
                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                if (uri != null) {
                    @Suppress("NewApi")
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                }

                val file = UniFile.fromUri(context, uri)
                preferences.downloadsDirectory().set(file.uri.toString())
            }
        }
    }

    fun predefinedDirectorySelected(selectedDir: String) {
        val path = Uri.fromFile(File(selectedDir))
        preferences.downloadsDirectory().set(path.toString())
    }

    fun customDirectorySelected() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try {
            startActivityForResult(intent, DOWNLOAD_DIR)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    class DownloadDirectoriesDialog(val controller: SettingsDownloadController) :
        MaterialAlertDialogBuilder(controller.activity!!.withOriginalWidth()) {

        private val preferences: PreferencesHelper = Injekt.get()

        val activity = controller.activity!!

        init {
            val currentDir = preferences.downloadsDirectory().get()
            val externalDirs =
                getExternalDirs() + File(activity.getString(R.string.custom_location))
            val selectedIndex = externalDirs.map(File::toString).indexOfFirst { it in currentDir }
            val items = externalDirs.map { it.path }

            setTitle(R.string.download_location)
            setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, position ->
                if (position == externalDirs.lastIndex) {
                    controller.customDirectorySelected()
                } else {
                    controller.predefinedDirectorySelected(items[position])
                }
                dialog.dismiss()
            }
            setNegativeButton(android.R.string.cancel, null)
        }

        private fun getExternalDirs(): List<File> {
            val defaultDir = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + activity.resources?.getString(R.string.app_name) +
                File.separator + "downloads"

            return mutableListOf(File(defaultDir)) +
                ContextCompat.getExternalFilesDirs(activity, "").filterNotNull()
        }
    }

    private companion object {
        const val DOWNLOAD_DIR = 104
    }
}
