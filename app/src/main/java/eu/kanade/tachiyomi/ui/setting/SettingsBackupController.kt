package eu.kanade.tachiyomi.ui.setting

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupConst
import eu.kanade.tachiyomi.data.backup.BackupCreateService
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupRestoreService
import eu.kanade.tachiyomi.data.backup.full.BackupFull
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.getFilePicker
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.requestPermissionsSafe
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsBackupController : SettingsController() {

    /**
     * Flags containing information of what to backup.
     */
    private var backupFlags = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 500)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.backup

        preference {
            key = "pref_create_backup"
            titleRes = R.string.create_backup
            summaryRes = R.string.can_be_used_to_restore

            onClick { backup(context, BackupConst.BACKUP_TYPE_FULL) }
        }
        preference {
            key = "pref_create_legacy_backup"
            titleRes = R.string.create_backup_legacy
            summaryRes = R.string.can_be_used_to_restore_legacy

            onClick { backup(context, BackupConst.BACKUP_TYPE_LEGACY) }
        }

        preference {
            titleRes = R.string.restore_backup
            summaryRes = R.string.restore_from_backup_file

            onClick {
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
            titleRes = R.string.service

            intListPreference(activity) {
                key = Keys.backupInterval
                titleRes = R.string.backup_frequency
                entriesRes = arrayOf(
                    R.string.manual, R.string.every_6_hours,
                    R.string.every_12_hours, R.string.daily,
                    R.string.every_2_days, R.string.weekly
                )
                entryValues = listOf(0, 6, 12, 24, 48, 168)
                defaultValue = 0

                onChange { newValue ->
                    // Always cancel the previous task, it seems that sometimes they are not updated
                    BackupCreatorJob.setupTask(0)

                    val interval = newValue as Int
                    if (interval > 0) {
                        BackupCreatorJob.setupTask(interval)
                    }
                    true
                }
            }
            val backupDir = preference {
                key = Keys.backupDirectory
                titleRes = R.string.backup_location

                onClick {
                    val currentDir = preferences.backupsDirectory().getOrDefault()
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        startActivityForResult(intent, CODE_BACKUP_DIR)
                    } catch (e: ActivityNotFoundException) {
                        // Fall back to custom picker on error
                        startActivityForResult(preferences.context.getFilePicker(currentDir), CODE_BACKUP_DIR)
                    }
                }

                preferences.backupsDirectory().asObservable()
                    .subscribeUntilDestroy { path ->
                        val dir = UniFile.fromUri(context, path.toUri())
                        summary = dir.filePath + "/automatic"
                    }
            }
            val backupNumber = intListPreference(activity) {
                key = Keys.numberOfBackups
                titleRes = R.string.max_auto_backups
                entries = listOf("1", "2", "3", "4", "5")
                entryRange = 1..5
                defaultValue = 1
            }

            preferences.backupInterval().asObservable()
                .subscribeUntilDestroy {
                    backupDir.isVisible = it > 0
                    backupNumber.isVisible = it > 0
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
        when (requestCode) {
            CODE_BACKUP_DIR -> if (data != null && resultCode == Activity.RESULT_OK) {
                val activity = activity ?: return
                // Get uri of backup folder.
                val uri = data.data

                // Get UriPermission so it's possible to write files
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                if (uri != null) {
                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                }

                // Set backup Uri
                preferences.backupsDirectory().set(uri.toString())
            }
            CODE_FULL_BACKUP_CREATE, CODE_LEGACY_BACKUP_CREATE -> if (data != null && resultCode == Activity.RESULT_OK) {
                val activity = activity ?: return

                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                if (uri != null) {
                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                }

                val file = UniFile.fromUri(activity, uri)

                activity.toast(R.string.creating_backup)

                val backuptype = if (requestCode == CODE_FULL_BACKUP_CREATE) BackupConst.BACKUP_TYPE_FULL else BackupConst.BACKUP_TYPE_LEGACY

                BackupCreateService.start(activity, file.uri, backupFlags, backuptype)
            }
            CODE_BACKUP_RESTORE -> if (data != null && resultCode == Activity.RESULT_OK) {
                val uri = data.data
                uri?.path?.let {
                    val fileName = DocumentFile.fromSingleUri(activity!!, uri)?.name ?: uri.toString()
                    val pair = when {
                        fileName.endsWith(".proto.gz") -> Pair(BackupConst.BACKUP_TYPE_FULL, false)
                        fileName.endsWith(".json") -> Pair(BackupConst.BACKUP_TYPE_LEGACY, true)
                        else -> null
                    }
                    if (pair == null) {
                        activity!!.toast(activity!!.getString(R.string.invalid_backup_file_type, fileName))
                    } else {
                        RestoreBackupDialog(uri, pair.first, pair.second).showDialog(router)
                    }
                }
            }
        }
    }

    fun createBackup(flags: Int, type: Int) {
        backupFlags = flags
        val code = when (type) {
            BackupConst.BACKUP_TYPE_FULL -> CODE_FULL_BACKUP_CREATE
            else -> CODE_LEGACY_BACKUP_CREATE
        }

        val fileName = when (type) {
            BackupConst.BACKUP_TYPE_FULL -> BackupFull.getDefaultFilename()
            else -> Backup.getDefaultFilename()
        }

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
                R.string.manga, R.string.categories, R.string.chapters,
                R.string.tracking, R.string.history
            )
                .map { activity.getString(it) }

            return MaterialDialog(activity)
                .title(R.string.create_backup)
                .message(R.string.what_should_backup)
                .listItemsMultiChoice(
                    items = options, disabledIndices = intArrayOf(0),
                    initialSelection = intArrayOf(0, 1, 2, 3, 4)
                ) { _, positions, _ ->
                    var flags = 0
                    for (i in 1 until positions.size) {
                        when (positions[i]) {
                            1 -> flags = flags or BackupCreateService.BACKUP_CATEGORY
                            2 -> flags = flags or BackupCreateService.BACKUP_CHAPTER
                            3 -> flags = flags or BackupCreateService.BACKUP_TRACK
                            4 -> flags = flags or BackupCreateService.BACKUP_HISTORY
                        }
                    }

                    (targetController as? SettingsBackupController)?.createBackup(flags, type)
                }
                .positiveButton(R.string.create)
                .negativeButton(android.R.string.cancel)
        }

        private companion object {
            const val KEY_TYPE = "CreateBackupDialog.type"
        }
    }

    class RestoreBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        constructor(uri: Uri, type: Int, isOnline: Boolean) : this(
            bundleOf(
                KEY_URI to uri,
                KEY_TYPE to type,
                KEY_MODE to isOnline
            )
        )

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {

            val activity = activity!!
            val uri: Uri = args.getParcelable(KEY_URI)!!
            val type: Int = args.getInt(KEY_TYPE)
            val isOnline: Boolean = args.getBoolean(KEY_MODE, true)

            return try {
                MaterialDialog(activity)
                    .title(R.string.restore_backup)
                    .message(R.string.restore_message)
                    .positiveButton(R.string.restore) {
                        val context = applicationContext
                        if (context != null) {
                            activity.toast(R.string.restoring_backup)
                            BackupRestoreService.start(context, uri, type, isOnline)
                        }
                    }
            } catch (e: Exception) {
                MaterialDialog(activity)
                    .title(R.string.invalid_backup_file)
                    .message(text = e.message)
                    .positiveButton(android.R.string.cancel)
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
