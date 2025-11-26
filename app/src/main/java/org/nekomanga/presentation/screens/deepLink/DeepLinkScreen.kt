package org.nekomanga.presentation.screens.deepLink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.util.system.toast
import org.nekomanga.presentation.screens.LoadingScreen
import org.nekomanga.presentation.screens.Screens

@Composable
fun DeepLinkScreen(
    host: String,
    path: String,
    id: String,
    deepLinkViewModel: DeepLinkViewModel,
    onNavigate: (List<NavKey>) -> Unit,
) {
    val deepLinkState by deepLinkViewModel.deepLinkState.collectAsState()

    LaunchedEffect(host, path, id) { deepLinkViewModel.handleDeepLink(host, path, id) }

    when (val state = deepLinkState) {
        is DeepLinkState.Loading -> LoadingScreen()
        is DeepLinkState.Error -> {
            LocalContext.current.toast(state.errorMessage)
            onNavigate(listOf(Screens.Browse()))
        }
        is DeepLinkState.Success -> {
            onNavigate(state.screens)
        }
    }
}
