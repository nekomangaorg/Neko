package org.nekomanga.presentation.screens.manga

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun MangaScreenTopBar(
        themeColorState: ThemeColorState,
        incognitoMode: Boolean
        onNavigationIconClick: () -> Unit,
        onSearch: (String?) -> Unit,
) {

}
