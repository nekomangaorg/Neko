package org.nekomanga.presentation.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tachiyomi.core.preference.Preference

@Composable
fun <T> Preference<T>.collectAsState(): State<T> {
    val flow = remember(this) { changes() }
    return flow.collectAsState(initial = get())
}

@Composable
fun <T> Preference<T>.collectAsStateWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): State<T> {
    val flow = remember(this) { changes() }
    return flow.collectAsStateWithLifecycle(initialValue = get(), minActiveState = minActiveState)
}
