package org.nekomanga.presentation.screens.settings

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
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.UiText

@Composable
fun SettingsMainScreen(modifier: Modifier = Modifier, onGeneralClick: () -> Unit) {
    NekoScaffold(
        type = NekoScaffoldType.SearchOutline,
        onNavigationIconClicked = {},
        onSearch = {},
        searchPlaceHolder = stringResource(id = R.string.search_settings),
        actions = {},
    ) { incomingPaddingValues ->
        LazyColumn(contentPadding = incomingPaddingValues, modifier = Modifier.fillMaxWidth()) {
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
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.library),
                    icon = Icons.Outlined.CollectionsBookmark,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.data_storage),
                    icon = Icons.Outlined.Folder,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.site_specific_settings),
                    icon = Icons.Outlined.Public,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.merge_source_settings),
                    icon = Icons.Outlined.Merge,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.reader),
                    icon = Icons.AutoMirrored.Default.ChromeReaderMode,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.downloads),
                    icon = Icons.Outlined.Download,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.tracked),
                    icon = Icons.Outlined.Autorenew,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.security),
                    icon = Icons.Outlined.Security,
                    onClick = {},
                )
            }
            item {
                IconItem(
                    labelText = UiText.StringResource(R.string.advanced),
                    icon = Icons.Outlined.Code,
                    onClick = {},
                )
            }
        }
    }
}
