package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.dialog.AddCategoryDialog
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

internal class AddEditCategoriesScreen(val onNavigationIconClick: () -> Unit) {

    @Composable
    fun Content() {

        var showAddDialog by remember { mutableStateOf(false) }

        if (showAddDialog) {
            AddCategoryDialog(
                defaultThemeColorState(),
                emptyList(),
                onDismiss = { showAddDialog = false },
                onConfirm = {},
            )
        }

        NekoScaffold(
            type = NekoScaffoldType.Title,
            title = stringResource(R.string.edit_categories),
            onNavigationIconClicked = onNavigationIconClick,
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {}

                ExtendedFloatingActionButton(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(
                                bottom = contentPadding.calculateBottomPadding(),
                                end = Size.small,
                            ),
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(text = stringResource(R.string.add)) },
                )
            }
        }
    }
}
