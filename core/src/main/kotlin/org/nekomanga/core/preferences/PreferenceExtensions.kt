package org.nekomanga.core.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.preference.Preference

fun Preference<Boolean>.toggle() = set(!get())

fun <T> Flow<T>.observeAndUpdate(scope: CoroutineScope, update: (T) -> Unit) {
    this.distinctUntilChanged().onEach { value -> update(value) }.launchIn(scope)
}
