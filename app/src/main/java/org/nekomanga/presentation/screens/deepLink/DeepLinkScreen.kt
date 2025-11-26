package org.nekomanga.presentation.screens.deepLink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.NavBackStackEntry
import eu.kanade.tachiyomi.util.manga.MangaMappings
import org.nekomanga.presentation.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun DeepLinkScreen(
    navBackStackEntry: NavBackStackEntry,
    deepLinkViewModel: DeepLinkViewModel =
        viewModel(factory = viewModelFactory { DeepLinkViewModel(Injekt.get<MangaMappings>()) }),
    onDeepLinkHandled: (String) -> Unit,
) {
    val deepLinkState by deepLinkViewModel.deepLinkState.collectAsState()

    val host = navBackStackEntry.arguments?.getString("host")
    val path = navBackStackEntry.arguments?.getString("path")
    val id = navBackStackEntry.arguments?.getString("id")

    LaunchedEffect(host, path, id) {
        if (host != null && path != null && id != null) {
            deepLinkViewModel.handleDeepLink(host, path, id)
        }
    }

    when (val state = deepLinkState) {
        is DeepLinkState.Loading -> LoadingScreen()
        is DeepLinkState.Success -> {
            LaunchedEffect(state.query) {
                onDeepLinkHandled(state.query)
            }
        }
    }
}
