package org.nekomanga.presentation.screens.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.bars.TitleTopAppBar
import org.nekomanga.presentation.functions.getTopAppBarColor

@Composable
fun AddEditCategoryTopBar(
    onNavigationIconClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor("", false)

    TitleTopAppBar(
        color = color,
        title = stringResource(R.string.edit_categories),
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        onNavigationIconClicked = onNavigationIconClicked,
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = false,
        scrollBehavior = scrollBehavior,
    )
}
