package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import org.nekomanga.R
import org.nekomanga.presentation.screens.license.LicenseTopAppBar
import org.nekomanga.presentation.theme.Size

@Composable
fun LicenseScreen(onBackPressed: () -> Unit) {

    val updateTopBar = LocalBarUpdater.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val screenBars = remember {
        ScreenBars(
            topBar = {
                LicenseTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onNavigationClicked = onBackPressed,
                )
            },
            scrollBehavior = scrollBehavior,
        )
    }
    DisposableEffect(Unit) {
        updateTopBar(screenBars)
        onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
    }

    val libraries by produceLibraries(R.raw.aboutlibraries)

    LibrariesContainer(
        libraries = libraries,
        padding = LibraryDefaults.libraryPadding(contentPadding = PaddingValues(Size.tiny)),
    )
}
