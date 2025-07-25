package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.preference.PreferenceScreen
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.BackupConst
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.disableItems
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import org.nekomanga.domain.backup.BackupPreferences
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.domain.storage.StoragePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsDataController : AbstractSettingsController() {
    private val storagePreferences: StoragePreferences = Injekt.get()
    private val storageManager: StorageManager = Injekt.get()
    private val backupPreferences: BackupPreferences = Injekt.get()

    /** Flags containing information of what to backup. */
    private var backupFlags = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.data_storage

            preference {
                titleRes = R.string.storage_location
                onClick { pickFileDirectory() }

                storagePreferences
                    .baseStorageDirectory()
                    .changes()
                    .onEach { path ->
                        val dir = UniFile.fromUri(context, path.toUri())!!
                        summary = dir.filePath ?: path
                    }
                    .launchIn(viewScope)
            }

            preference {
                key = "pref_create_backup"
                titleRes = R.string.create_backup
                summaryRes = R.string.can_be_used_to_restore

                onClick {
                    if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                        context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                    }

                    if (!BackupCreatorJob.isManualJobRunning(context)) {
                        val ctrl = CreateBackupDialogLegacy()
                        ctrl.targetController = this@SettingsDataController
                        ctrl.showDialog(router)
                    } else {
                        context.toast(R.string.backup_in_progress)
                    }
                }
            }

            preference {
                key = "pref_restore_backup"
                titleRes = R.string.restore_backup
                summaryRes = R.string.restore_from_backup_file

                onClick {
                    if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                        context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                    }

                    if (!BackupRestoreJob.isRunning(context)) {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.type = "*/*"
                        val title = resources?.getString(R.string.select_backup_file)
                        val chooser = Intent.createChooser(intent, title)
                        startActivityForResult(chooser, CODE_BACKUP_RESTORE)
                    } else {
                        context.toast(R.string.restore_in_progress)
                    }
                }
            }

            preferenceCategory {
                titleRes = R.string.automatic_backups

                intListPreference(activity) {
                    bindTo(backupPreferences.backupInterval())
                    titleRes = R.string.backup_frequency
                    entriesRes =
                        arrayOf(
                            R.string.off,
                            R.string.every_6_hours,
                            R.string.every_12_hours,
                            R.string.daily,
                            R.string.every_2_days,
                            R.string.weekly,
                        )
                    entryValues = listOf(0, 6, 12, 24, 48, 168)

                    onChange { newValue ->
                        val interval = newValue as Int
                        when (interval > 0) {
                            true -> BackupCreatorJob.setupTask(context, interval)
                            false -> BackupCreatorJob.cancelTask(context)
                        }
                        BackupCreatorJob.setupTask(context, interval)
                        true
                    }
                }
            }

            infoPreference(R.string.backup_info)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            val activity = activity ?: return
            val uri = data.data

            if (uri == null) {
                activity.toast(R.string.backup_restore_invalid_uri)
                return
            }

            when (requestCode) {
                STORAGE_DIR -> {
                    val context = applicationContext ?: return
                    val uri = data.data
                    val flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    if (uri != null) {
                        context.contentResolver.takePersistableUriPermission(uri, flags)
                    }

                    val file = UniFile.fromUri(context, uri)!!

                    storagePreferences.baseStorageDirectory().set(file.uri.toString())
                }
                CODE_BACKUP_CREATE -> {
                    val flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                    activity.toast(R.string.creating_backup)
                    BackupCreatorJob.startNow(activity, uri, backupFlags)
                }
                CODE_BACKUP_RESTORE -> {
                    RestoreBackupDialog(uri).showDialog(router)
                }
            }
        }
    }

    fun createBackup(flags: Int) {
        backupFlags = flags
        try {
            // Use Android's built-in file creator
            val intent =
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    setDataAndType(storageManager.getBackupDirectory()!!.uri, "application/*")
                    putExtra(Intent.EXTRA_TITLE, Backup.getBackupFilename())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            storageManager.getBackupDirectory()!!.uri,
                        )
                    }
                }

            startActivityForResult(intent, CODE_BACKUP_CREATE)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    class CreateBackupDialogLegacy(bundle: Bundle? = null) : DialogController(bundle) {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val options =
                arrayOf(
                        R.string.manga,
                        R.string.categories,
                        R.string.chapters,
                        R.string.tracking,
                        R.string.history,
                        R.string.all_read_manga,
                    )
                    .map { activity.getString(it) }

            return activity
                .materialAlertDialog()
                .setTitle(R.string.what_should_backup)
                .setMultiChoiceItems(
                    options.toTypedArray(),
                    options.map { true }.toBooleanArray(),
                ) { dialog, position, _ ->
                    if (position == 0) {
                        val listView = (dialog as AlertDialog).listView
                        listView.setItemChecked(position, true)
                    }
                }
                .setPositiveButton(R.string.create) { dialog, _ ->
                    val listView = (dialog as AlertDialog).listView
                    var flags = 0
                    for (i in 1 until listView.count) {
                        if (listView.isItemChecked(i)) {
                            when (i) {
                                1 -> flags = flags or BackupConst.BACKUP_CATEGORY
                                2 -> flags = flags or BackupConst.BACKUP_CHAPTER
                                3 -> flags = flags or BackupConst.BACKUP_TRACK
                                4 -> flags = flags or BackupConst.BACKUP_HISTORY
                                5 -> flags = flags or BackupConst.BACKUP_READ_MANGA
                            }
                        }
                    }
                    (targetController as? SettingsDataController)?.createBackup(flags)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply { disableItems(arrayOf(options.first())) }
        }
    }

    class RestoreBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        constructor(uri: Uri) : this(bundleOf(KEY_URI to uri))

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val uri: Uri = args.getParcelable(KEY_URI)!!

            return try {
                val type = BackupConst.BACKUP_TYPE_FULL
                val results = BackupFileValidator().validate(activity, uri)

                var message = activity.getString(R.string.restore_neko)

                if (results.missingMangaDexEntries) {
                    message += "\n\nNo MangaDex manga found in the backup."
                }

                if (results.missingTrackers.isNotEmpty()) {
                    message +=
                        "\n\n${activity.getString(R.string.restore_missing_trackers)}\n${
                        results.missingTrackers.joinToString("\n") { "- $it" }
                    }"
                }

                return activity
                    .materialAlertDialog()
                    .setTitle(R.string.restore_backup)
                    .setMessage(message)
                    .setPositiveButton(R.string.restore) { _, _ ->
                        val context = applicationContext
                        if (context != null) {
                            activity.toast(R.string.restoring_backup)
                            BackupRestoreJob.start(context, uri)
                        }
                    }
                    .create()
            } catch (e: Exception) {
                activity
                    .materialAlertDialog()
                    .setTitle(R.string.invalid_backup_file)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.cancel, null)
                    .create()
            }
        }
    }

    private fun pickFileDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try {
            startActivityForResult(intent, STORAGE_DIR)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    companion object {
        const val STORAGE_DIR = 104
        const val KEY_URI = "RestoreBackupDialog.uri"
        const val CODE_BACKUP_CREATE = 504
        const val CODE_BACKUP_RESTORE = 505
    }
}
