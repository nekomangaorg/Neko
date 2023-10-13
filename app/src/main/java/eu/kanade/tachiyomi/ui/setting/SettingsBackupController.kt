package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.preference.PreferenceScreen
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupConst
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.BackupRestoreService
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.disableItems
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.requestFilePermissionsSafe
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

                if (!BackupCreatorJob.isManualJobRunning(context)) {
                    val ctrl = CreateBackupDialog()
                    ctrl.targetController = this@SettingsBackupController
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

                if (!BackupRestoreService.isRunning(context)) {
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
                bindTo(preferences.backupInterval())
                titleRes = R.string.backup_frequency
                entriesRes = arrayOf(
                    R.string.manual,
                    R.string.every_6_hours,
                    R.string.every_12_hours,
                    R.string.daily,
                    R.string.every_2_days,
                    R.string.weekly,
                )
                entryValues = listOf(0, 6, 12, 24, 48, 168)

                onChange { newValue ->
                    val interval = newValue as Int
                    BackupCreatorJob.setupTask(context, interval)
                    true
                }
            }
            preference {
                bindTo(preferences.backupsDirectory())
                titleRes = R.string.backup_location

                onClick {
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        startActivityForResult(intent, CODE_BACKUP_DIR)
                    } catch (e: ActivityNotFoundException) {
                        activity?.toast(R.string.file_picker_error)
                    }
                }

                preferences.backupInterval().changes().onEach { isVisible = it > 0 }.launchIn(viewScope)

                preferences.backupsDirectory().changes()
                    .onEach { path ->
                        val dir = UniFile.fromUri(context, path.toUri())
                        summary = dir.filePath + "/automatic"
                    }.launchIn(viewScope)
            }
            intListPreference(activity) {
                bindTo(preferences.numberOfBackups())
                titleRes = R.string.max_auto_backups
                entries = (1..5).map(Int::toString)
                entryRange = 1..5
                preferences.backupInterval().changes().onEach { isVisible = it > 0 }.launchIn(viewScope)
            }
        }

        infoPreference(R.string.backup_info)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_backup, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_backup_help -> activity?.openInBrowser(HELP_URL)
        }
        return super.onOptionsItemSelected(item)
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
                CODE_BACKUP_DIR -> {
                    // Get UriPermission so it's possible to write files
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    activity.contentResolver.takePersistableUriPermission(uri, flags)
                    preferences.backupsDirectory().set(uri.toString())
                }

                CODE_BACKUP_CREATE -> {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
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
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/*")
                .putExtra(Intent.EXTRA_TITLE, Backup.getBackupFilename())

            startActivityForResult(intent, CODE_BACKUP_CREATE)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    class CreateBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val options = arrayOf(
                R.string.manga,
                R.string.categories,
                R.string.chapters,
                R.string.tracking,
                R.string.history,
                R.string.all_read_manga,
            )
                .map { activity.getString(it) }

            return activity.materialAlertDialog()
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
                    (targetController as? SettingsBackupController)?.createBackup(flags)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create().apply {
                    disableItems(arrayOf(options.first()))
                }
        }
    }

    class RestoreBackupDialog(bundle: Bundle? = null) : DialogController(bundle) {
        constructor(uri: Uri) : this(
            bundleOf(KEY_URI to uri),
        )

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
                            BackupRestoreService.start(context, uri, type)
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
    }
}

private const val KEY_URI = "RestoreBackupDialog.uri"

private const val CODE_BACKUP_DIR = 503
private const val CODE_BACKUP_CREATE = 504
private const val CODE_BACKUP_RESTORE = 505

private const val HELP_URL = "https://tachiyomi.org/docs/guides/backups"
