package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
