package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.ui.setting.CacheData
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.dialog.CreateBackupDialog
import org.nekomanga.presentation.components.dialog.RestoreDialog
import org.nekomanga.presentation.components.storage.storageLocationPicker
import org.nekomanga.presentation.components.storage.storageLocationText
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.presentation.theme.Size

internal class DataStorageSettingsScreen(
    val storagePreferences: StoragePreferences,
    val cacheData: CacheData,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.data_storage

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        val context = LocalContext.current
        val pickStorageLocation = storageLocationPicker(storagePreferences.baseStorageDirectory())

        return persistentListOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.storage_location),
                subtitle = storageLocationText(storagePreferences.baseStorageDirectory()),
                onClick = { pickStorageLocation.launch(null) },
            ),
            backupAndRestoreGroup(context),
            dataManagementGroup(context, cacheData),
        )
    }

    @Composable
    private fun backupAndRestoreGroup(context: Context): Preference.PreferenceGroup {
        var showCreateBackupDialog by rememberSaveable { mutableStateOf(false) }
        var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
        var restoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }

        var pendingBackupFlags: Int? by remember { mutableStateOf(null) }

        val chooseBackupDir =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/*"),
                onResult = { uri: Uri? ->
                    if (uri != null && pendingBackupFlags != null) {
                        BackupCreatorJob.startNow(context, uri, pendingBackupFlags!!)
                        pendingBackupFlags = null
                    }
                },
            )

        val chooseRestoreFile =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri: Uri? ->
                    if (uri != null) {
                        restoreUri = uri
                        showRestoreDialog = true
                    }
                },
            )

        if (showCreateBackupDialog) {
            CreateBackupDialog(
                onDismiss = { showCreateBackupDialog = false },
                onConfirm = { backupFlags ->
                    pendingBackupFlags = backupFlags
                    chooseBackupDir.launch(Backup.getBackupFilename())
                },
            )
        }
        if (showRestoreDialog) {
            RestoreDialog(
                uri = restoreUri!!,
                onDismiss = { showRestoreDialog = false },
                onConfirm = {
                    BackupRestoreJob.start(context, restoreUri!!)
                    restoreUri == null
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.backup_and_restore),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.create_backup),
                        subtitle = stringResource(R.string.can_be_used_to_restore),
                        onClick = {
                            if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                                context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                            }
                            if (!BackupCreatorJob.isManualJobRunning(context)) {
                                showCreateBackupDialog = !showCreateBackupDialog
                            } else {
                                context.toast(R.string.backup_in_progress)
                            }
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.restore_backup),
                        subtitle = stringResource(R.string.restore_from_backup_file),
                        onClick = {
                            if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                                context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                            }
                            if (!BackupRestoreJob.isRunning(context)) {
                                chooseRestoreFile.launch("*/*")
                            } else {
                                context.toast(R.string.restore_in_progress)
                            }
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = storagePreferences.backupInterval(),
                        title = stringResource(R.string.automatic_backups),
                        entries =
                            persistentMapOf(
                                0 to stringResource(R.string.off),
                                6 to stringResource(R.string.every_6_hours),
                                12 to stringResource(R.string.every_12_hours),
                                24 to stringResource(R.string.daily),
                                48 to stringResource(R.string.every_2_days),
                                168 to stringResource(R.string.weekly),
                            ),
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(R.string.backup_info)),
                ),
        )
    }

    @Composable
    private fun dataManagementGroup(
        context: Context,
        cacheData: CacheData,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.data_management),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(R.string.total_cache_usage),
                        content = {
                            Column(modifier = Modifier.padding(start = Size.medium)) {
                                Text(
                                    text = "Parent cache folder: ${cacheData.parentCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Chapter disk cache: ${cacheData.chapterDiskCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Cover cache: ${cacheData.coverCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Custom cover cache: ${cacheData.customCoverCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Online cover cache: ${cacheData.onlineCoverCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Image cache: ${cacheData.imageCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Network cache: ${cacheData.networkCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                                Text(
                                    text = "Temp files: ${cacheData.tempFileCacheSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaLowContrast
                                        ),
                                )
                            }
                        },
                    )
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf(
                SearchTerm(stringResource(R.string.storage_location)),
                SearchTerm(
                    stringResource(R.string.create_backup),
                    stringResource(R.string.can_be_used_to_restore),
                ),
                SearchTerm(
                    stringResource(R.string.restore_backup),
                    stringResource(R.string.restore_from_backup_file),
                ),
                SearchTerm(stringResource(R.string.automatic_backups)),
                SearchTerm(stringResource(R.string.total_cache_usage)),
            )
        }
    }
}
