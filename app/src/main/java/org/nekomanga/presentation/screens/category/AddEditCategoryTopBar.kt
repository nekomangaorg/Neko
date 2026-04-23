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
    onNavigationIconClicked: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (color, onColor, useDarkIcons) = getTopAppBarColor(true, false)

    TitleTopAppBar(
        color = color,
        title = stringResource(R.string.edit_categories),
        navigationIcon =
            if (onNavigationIconClicked != null) Icons.AutoMirrored.Default.ArrowBack else null,
        onNavigationIconClicked = onNavigationIconClicked ?: {},
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = false,
        scrollBehavior = scrollBehavior,
    )
}
