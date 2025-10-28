package org.nekomanga.presentation.screens.manga

import androidx.compose.runtime.Composable
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun MangaScreenTopBar(
    themeColorState: ThemeColorState,
    incognitoMode: Boolean,
    onNavigationIconClick: () -> Unit,
    onSearch: (String?) -> Unit,
) {}
