package org.nekomanga.presentation.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText

@Composable
fun SettingsMainScreen(
    contentPadding: PaddingValues,
    onGeneralClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onDataStorageClick: () -> Unit,
    onSiteSpecificClick: () -> Unit,
    onMergeSourceClick: () -> Unit,
    onReaderClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onAdvancedClick: () -> Unit,
) {
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.general),
                icon = Icons.Outlined.Tune,
                onClick = onGeneralClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.appearance),
                icon = Icons.Outlined.Palette,
                onClick = onAppearanceClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.library),
                icon = Icons.Outlined.CollectionsBookmark,
                onClick = onLibraryClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.data_storage),
                icon = Icons.Outlined.Folder,
                onClick = onDataStorageClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.site_specific_settings),
                icon = Icons.Outlined.Public,
                onClick = onSiteSpecificClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.merge_source_settings),
                icon = Icons.Outlined.Merge,
                onClick = onMergeSourceClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.reader),
                icon = Icons.AutoMirrored.Default.ChromeReaderMode,
                onClick = onReaderClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.downloads),
                icon = Icons.Outlined.Download,
                onClick = onDownloadsClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.tracked),
                icon = Icons.Outlined.Autorenew,
                onClick = onTrackingClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.security),
                icon = Icons.Outlined.Security,
                onClick = onSecurityClick,
            )
        }
        item {
            IconItem(
                labelText = UiText.StringResource(R.string.advanced),
                icon = Icons.Outlined.Code,
                onClick = onAdvancedClick,
            )
        }
    }
}
