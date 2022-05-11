package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceScreen
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupConst
import eu.kanade.tachiyomi.data.backup.BackupCreateService
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupRestoreService
import eu.kanade.tachiyomi.data.backup.full.FullBackupRestoreValidator
import eu.kanade.tachiyomi.data.backup.full.models.BackupFull
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.disableItems
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.requestFilePermissionsSafe
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsBackupController : SettingsController() {

    /**
     * Flags containing information of what to backup.
     */
    private var backupFlags = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestFilePermissionsSafe(500, preferences)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.backup_and_restore

        preference {
            key = "pref_create_backup"
            titleRes = R.string.create_backup
            summaryRes = R.string.can_be_used_to_restore

            onClick {
                if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                    context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                }

                backup(context, BackupConst.BACKUP_TYPE_FULL)
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

                if (!BackupRestoreService.isRunning(context)) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/*"
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
                key = Keys.backupInterval
                titleRes = R.string.backup_frequency
                entriesRes = arrayOf(
                    R.string.manual,
                    R.string.every_6_hours,
                    R.string.every_12_hours,
                    R.string.daily,
                    R.string.every_2_days,
                    R.string.weekly
                )
                entryValues = listOf(0, 6, 12, 24, 48, 168)
                defaultValue = 0

                onChange { newValue ->
                    // Always cancel the previous task, it seems that sometimes they are not updated

                    val interval = newValue as Int
                    BackupCreatorJob.setupTask(context, interval)
                    true
                }
            }
            preference {
                key = Keys.backupDirectory
                titleRes = R.string.backup_location

                onClick {
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        startActivityForResult(intent, CODE_BACKUP_DIR)
                    } catch (e: ActivityNotFoundException) {
                        activity?.toast(R.string.file_picker_error)
                    }
                }

                preferences.backupsDirectory().asFlow()
                    .onEach { path ->
                        val dir = UniFile.fromUri(context, path.toUri())
                        summary = dir.filePath + "/automatic"
                    }
                    .launchIn(viewScope)
                preferences.backupInterval().asImmediateFlow { isVisible = it > 0 }
                    .launchIn(viewScope)
            }
            intListPreference(activity) {
                key = Keys.numberOfBackups
                titleRes = R.string.max_auto_backups
                entries = listOf("1", "2", "3", "4", "5")
                entryRange = 1..5
                defaultValue = 1

                preferences.backupInterval().asImmediateFlow { isVisible = it > 0 }
                    .launchIn(viewScope)
            }
        }
    }

    private fun backup(context: Context, type: Int) {
        if (!BackupCreateService.isRunning(context)) {
            val ctrl = CreateBackupDialog(type)
            ctrl.targetController = this@SettingsBackupController
            ctrl.showDialog(router)
        } else {
            context.toast(R.string.backup_in_progress)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            val activity = activity ?: return
            val uri = data.data
            when (requestCode) {
                CODE_BACKUP_DIR -> {
                    // Get UriPermission so it's possible to write files
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    if (uri != null) {
                        activity.contentResolver.takePersistableUriPermission(uri, flags)
                    }

                    // Set backup Uri
                    preferences.backupsDirectory().set(uri.toString())
                }
                CODE_FULL_BACKUP_CREATE, CODE_LEGACY_BACKUP_CREATE -> {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    if (uri != null) {
                        activity.contentResolver.takePersistableUriPermission(uri, flags)
                    }

                    val file = UniFile.fromUri(activity, uri)

                    activity.toast(R.string.creating_backup)

                    BackupCreateService.start(
                        activity,
                        file.uri,
                        backupFlags,
                        BackupConst.BACKUP_TYPE_FULL
                    )
                }
                CODE_BACKUP_RESTORE -> {
                    uri?.path?.let {
                        val fileName =
                            DocumentFile.fromSingleUri(activity, uri)?.name ?: uri.toString()
                        when {
                            fileName.endsWith(".proto.gz") -> {
                                RestoreBackupDialog(
                                    uri,
                                    BackupConst.BACKUP_TYPE_FULL
                                ).showDialog(router)
                            }
                            else -> {
                                activity.toast(
                                    activity.getString(
                                        R.string.invalid_backup_file_type,
                                        fileName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun createBackup(flags: Int, type: Int) {
        backupFlags = flags

        val code = CODE_FULL_BACKUP_CREATE

        val fileName = BackupFull.getDefaultFilename()
        // else -> Backup.getDefaultFilename()

        // Setup custom file picker intent
        // Get dirs
        val currentDir = preferences.backupsDirectory().get()

        try {
            // Use Android's built-in file creator
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
                .putExtra(Intent.EXTRA_TITLE, fileName)

            startActivityForResult(intent, code)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    class CreateBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        constructor(type: Int) : this(
            bundleOf(
                KEY_TYPE to type
            )
        )

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val type = args.getInt(KEY_TYPE)
            val activity = activity!!
            val options = arrayOf(
                R.string.manga,
                R.string.categories,
                R.string.chapters,
                R.string.tracking,
                R.string.history,
            )
                .map { activity.getString(it) }

            return activity.materialAlertDialog()
                .setTitle(R.string.what_should_backup)
                .setMultiChoiceItems(
                    options.toTypedArray(),
                    booleanArrayOf(true, true, true, true, true, true)
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
                                1 -> flags = flags or BackupCreateService.BACKUP_CATEGORY
                                2 -> flags = flags or BackupCreateService.BACKUP_CHAPTER
                                3 -> flags = flags or BackupCreateService.BACKUP_TRACK
                                4 -> flags = flags or BackupCreateService.BACKUP_HISTORY
                            }
                        }
                    }
                    (targetController as? SettingsBackupController)?.createBackup(flags, type)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create().apply {
                    disableItems(arrayOf(options.first()))
                }
        }

        private companion object {
            const val KEY_TYPE = "CreateBackupDialog.type"
        }
    }

    class RestoreBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        constructor(uri: Uri, type: Int) : this(
            bundleOf(
                KEY_URI to uri,
                KEY_TYPE to type
            )
        )

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val uri: Uri = args.getParcelable(KEY_URI)!!
            val type: Int = args.getInt(KEY_TYPE)
            val isOnline: Boolean = args.getBoolean(KEY_MODE, true)

            return try {
                var message = activity.getString(R.string.restore_neko)

                val validator = FullBackupRestoreValidator()

                val results = validator.validate(activity, uri)
                if (results.missingSources.isNotEmpty()) {
                    message += "\n\n${activity.getString(R.string.restore_missing_sources)}\n${
                        results.missingSources.joinToString("\n") { "- $it" }
                    }"
                }
                if (results.missingTrackers.isNotEmpty()) {
                    message += "\n\n${activity.getString(R.string.restore_missing_trackers)}\n${
                        results.missingTrackers.joinToString("\n") { "- $it" }
                    }"
                }

                return activity.materialAlertDialog()
                    .setTitle(R.string.restore_backup)
                    .setMessage(message)
                    .setPositiveButton(R.string.restore) { _, _ ->
                        val context = applicationContext
                        if (context != null) {
                            activity.toast(R.string.restoring_backup)
                            BackupRestoreService.start(context, uri, type, isOnline)
                        }
                    }.create()
            } catch (e: Exception) {
                activity.materialAlertDialog()
                    .setTitle(R.string.invalid_backup_file)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.cancel, null)
                    .create()
            }
        }

        private companion object {
            const val KEY_URI = "RestoreBackupDialog.uri"
            const val KEY_TYPE = "RestoreBackupDialog.type"
            const val KEY_MODE = "RestoreBackupDialog.mode"
        }
    }

    private companion object {
        const val CODE_LEGACY_BACKUP_CREATE = 501
        const val CODE_BACKUP_DIR = 503
        const val CODE_FULL_BACKUP_CREATE = 504
        const val CODE_BACKUP_RESTORE = 505
    }
}
