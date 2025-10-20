package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType

@Composable
fun LibraryUpdateErrorsScreen(
    onNavigationIconClick: () -> Unit,
    preferencesHelper: PreferencesHelper,
) {
    NekoScaffold(
        type = NekoScaffoldType.Title,
        onNavigationIconClicked = onNavigationIconClick,
        title = stringResource(id = R.string.library_update_errors),
    ) { contentPadding ->
        val errors = preferencesHelper.libraryUpdateErrors().get().toList()
        LazyColumn(modifier = Modifier.padding(contentPadding)) {
            items(errors) { error -> Text(text = error, modifier = Modifier.padding(8.dp)) }
        }
    }
}
