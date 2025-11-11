package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import org.nekomanga.R
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.license.LicenseTopAppBar
import org.nekomanga.presentation.theme.Size

@Composable
fun LicenseScreen(onBackPressed: () -> Unit) {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val libraries by produceLibraries(R.raw.aboutlibraries)

    ChildScreenScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            LicenseTopAppBar(scrollBehavior = scrollBehavior, onNavigationClicked = onBackPressed)
        },
    ) { contentPadding ->
        LibrariesContainer(
            libraries = libraries,
            contentPadding = contentPadding,
            padding = LibraryDefaults.libraryPadding(contentPadding = PaddingValues(Size.tiny)),
        )
    }
}
