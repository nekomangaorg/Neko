package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.nekomanga.presentation.components.ButtonGroup
import org.nekomanga.presentation.theme.Size
import com.hippo.unifile.UniFile
import java.io.File
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.ui.setting.CacheData
import eu.kanade.tachiyomi.ui.setting.CacheType
import eu.kanade.tachiyomi.util.system.MiuiUtil
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.toTimestampString
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.CreateBackupDialog
import org.nekomanga.presentation.components.dialog.RestoreDialog
import org.nekomanga.presentation.components.storage.storageLocationPicker
import org.nekomanga.presentation.components.storage.storageLocationText
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import org.nekomanga.usecases.preferences.GetDateFormatUseCase
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class DataStorageSettingsScreen(
    incognitoMode: Boolean,
    val storagePreferences: StoragePreferences,
    val cacheData: CacheData,
    val clearCache: (CacheType) -> Unit,
    val toastEvent: SharedFlow<UiText.StringResource>,
    onNavigationIconClick: (() -> Unit)?,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {

    override fun getTitleRes(): Int = R.string.data_storage

    @Composable
    override fun getPreferences(): PersistentList<Preference> {
        val context = LocalContext.current

        LaunchedEffect(Unit) { toastEvent.collect { event -> context.toast(event.resourceId) } }

        val pickStorageLocation = storageLocationPicker(storagePreferences.baseStorageDirectory())

        return persistentListOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.storage_location),
                subtitle = storageLocationText(storagePreferences.baseStorageDirectory()),
                onClick = { pickStorageLocation.launch(null) },
            ),
            backupAndRestoreGroup(context),
            storageGroup(context),
            cacheGroup(context, cacheData, clearCache),
        )
    }

    @Composable
    private fun storageGroup(context: Context): Preference.PreferenceGroup {
        val storages = remember { DiskUtil.getExternalStorages(context) }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.storage_usage),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.InfoPreference(stringResource(R.string.storage_usage_info)),
                    Preference.PreferenceItem.CustomPreference(
                        title = "",
                        content = {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = Size.medium, vertical = Size.small),
                                verticalArrangement = Arrangement.spacedBy(Size.small),
                            ) {
                                storages.forEach { file ->
                                    val available = remember(file) { DiskUtil.getAvailableStorageSpace(file) }
                                    val availableText = remember(available) { Formatter.formatFileSize(context, available) }
                                    val total = remember(file) { DiskUtil.getTotalStorageSpace(file) }
                                    val totalText = remember(total) { Formatter.formatFileSize(context, total) }
                                    val progress = if (total > 0L) (1 - (available / total.toFloat())) else 0f

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Size.tiny),
                                    ) {
                                        Text(
                                            text = file.absolutePath,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )

                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .height(Size.small)
                                                    .clip(RoundedCornerShape(Size.tiny)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )

                                        Text(
                                            text = stringResource(R.string.available_disk_space_info, availableText, totalText),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                    )
                ),
        )
    }

    @Composable
    private fun backupAndRestoreGroup(context: Context): Preference.PreferenceGroup {
        val lastAutoBackup by storagePreferences.lastAutoBackupTimestamp().collectAsState()
        val getDateFormatUseCase = remember { Injekt.get<GetDateFormatUseCase>() }
        val formattedTime = remember(lastAutoBackup) {
            if (lastAutoBackup == 0L) {
                context.getString(R.string.never)
            } else {
                java.util.Date(lastAutoBackup).toTimestampString(getDateFormatUseCase())
            }
        }
        val backupInfoText = stringResource(R.string.backup_info) + "\n\n" + stringResource(R.string.last_auto_backup_info, formattedTime)

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
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(R.string.backup),
                        content = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = Size.medium),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                ButtonGroup(
                                    items = listOf("create", "restore"),
                                    selectedItem = "",
                                    onItemClick = { action ->
                                        if (action == "create") {
                                            if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                                                context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                                            }
                                            if (!BackupCreatorJob.isManualJobRunning(context)) {
                                                showCreateBackupDialog = !showCreateBackupDialog
                                            } else {
                                                context.toast(R.string.backup_in_progress)
                                            }
                                        } else {
                                            if (MiuiUtil.isMiui() && MiuiUtil.isMiuiOptimizationDisabled()) {
                                                context.toast(R.string.restore_miui_warning, Toast.LENGTH_LONG)
                                            }
                                            if (!BackupRestoreJob.isRunning(context)) {
                                                chooseRestoreFile.launch("*/*")
                                            } else {
                                                context.toast(R.string.restore_in_progress)
                                            }
                                        }
                                    },
                                ) { action ->
                                    val text = if (action == "create") stringResource(R.string.create) else stringResource(R.string.restore)
                                    Text(
                                        text = text,
                                        modifier = Modifier.padding(horizontal = Size.huge, vertical = Size.tiny),
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
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
                    Preference.PreferenceItem.InfoPreference(backupInfoText),
                ),
        )
    }

    @Composable
    private fun cacheGroup(
        context: Context,
        cacheData: CacheData,
        clearCache: (CacheType) -> Unit,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.cache),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.parent_cache_folder),
                        subtitle = stringResource(R.string.used_, cacheData.parentCacheSize),
                        onClick = { clearCache(CacheType.Parent) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.chapter_disk_cache),
                        subtitle = stringResource(R.string.used_, cacheData.chapterDiskCacheSize),
                        onClick = { clearCache(CacheType.ChapterDisk) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.cover_cache),
                        subtitle = stringResource(R.string.used_, cacheData.coverCacheSize),
                        onClick = { clearCache(CacheType.Cover) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.custom_cover_cache),
                        subtitle = stringResource(R.string.used_, cacheData.customCoverCacheSize),
                        onClick = { clearCache(CacheType.CustomCover) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.online_cover_cache),
                        subtitle = stringResource(R.string.used_, cacheData.onlineCoverCacheSize),
                        onClick = { clearCache(CacheType.OnlineCover) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.image_cache),
                        subtitle = stringResource(R.string.used_, cacheData.imageCacheSize),
                        onClick = { clearCache(CacheType.Image) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.network_cache),
                        subtitle = stringResource(R.string.used_, cacheData.networkCacheSize),
                        onClick = { clearCache(CacheType.Network) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.temp_file_cache),
                        subtitle = stringResource(R.string.used_, cacheData.tempFileCacheSize),
                        onClick = { clearCache(CacheType.Temp) },
                    ),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(title = stringResource(R.string.storage_location)),
                SearchTerm(
                    title = stringResource(R.string.storage_usage),
                    group = stringResource(R.string.storage_usage),
                ),
                SearchTerm(
                    title = stringResource(R.string.backup),
                    group = stringResource(R.string.backup_and_restore),
                ),
                SearchTerm(
                    title = stringResource(R.string.automatic_backups),
                    group = stringResource(R.string.backup_and_restore),
                ),
                SearchTerm(
                    title = stringResource(R.string.parent_cache_folder),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.chapter_disk_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.cover_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.custom_cover_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.online_cover_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.image_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.network_cache),
                    group = stringResource(R.string.cache),
                ),
                SearchTerm(
                    title = stringResource(R.string.temp_file_cache),
                    group = stringResource(R.string.cache),
                ),
            )
        }
    }
}
