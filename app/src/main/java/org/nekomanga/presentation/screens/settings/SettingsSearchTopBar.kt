package org.nekomanga.presentation.screens.settings

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.bars.SearchOutlineTopAppBar
import org.nekomanga.presentation.components.getTopAppBarColor

@Composable
fun SettingsSearchTopBar(
    onSearch: (String?) -> Unit,
    incognitoMode: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)

    SearchOutlineTopAppBar(
        onSearch = onSearch,
        searchPlaceHolder = stringResource(R.string.search_settings),
        color = color,
        incognitoMode = incognitoMode,
        scrollBehavior = scrollBehavior,
    )
}
