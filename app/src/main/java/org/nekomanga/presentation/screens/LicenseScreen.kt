package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import org.nekomanga.R
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.license.LicenseTopAppBar
import org.nekomanga.presentation.theme.Size

/**
 * LicenseScreen displays the open-source libraries used in the application.
 *
 * This screen-level Composable is responsive to [WindowSizeClass]. On expanded screens
 * (tablets/foldables), the layout limits the maximum width of the dependency list to
 * 800.dp and centers it, preventing the list items from stretching uncomfortably wide
 * and providing a more polished, premium user experience.
 *
 * @param windowSizeClass The screen's window size class constraints used to determine adaptive styling.
 * @param onBackPressed Callback invoked when the user navigates back.
 */
@Composable
fun LicenseScreen(windowSizeClass: WindowSizeClass, onBackPressed: () -> Unit) {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val libraries by produceLibraries(R.raw.aboutlibraries)

    val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    ChildScreenScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            LicenseTopAppBar(scrollBehavior = scrollBehavior, onNavigationClicked = onBackPressed)
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LibrariesContainer(
                modifier = if (isTablet) Modifier.widthIn(max = 800.dp) else Modifier.fillMaxSize(),
                libraries = libraries,
                contentPadding = contentPadding,
                padding = LibraryDefaults.libraryPadding(contentPadding = PaddingValues(Size.tiny)),
            )
        }
    }
}
