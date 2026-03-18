package org.nekomanga.core.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import tachiyomi.core.preference.Preference

fun Preference<Boolean>.toggle() = set(!get())

fun <S, T> Flow<T>.observeAndUpdate(
    stateFlow: MutableStateFlow<S>,
    scope: CoroutineScope,
    update: (S, T) -> S,
) {
    this.distinctUntilChanged()
        .onEach { value -> stateFlow.update { state -> update(state, value) } }
        .launchIn(scope)
}
