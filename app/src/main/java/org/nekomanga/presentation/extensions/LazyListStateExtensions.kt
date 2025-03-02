package org.nekomanga.presentation.extensions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
fun LazyListState.isScrolledToStart(): Boolean {
    return remember {
            derivedStateOf {
                val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()
                firstItem == null || firstItem.offset == layoutInfo.viewportStartOffset
            }
        }
        .value
}

@Composable
fun LazyListState.isScrolledToEnd(): Boolean {
    return remember {
            derivedStateOf {
                val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastItem == null || lastItem.size + lastItem.offset <= layoutInfo.viewportEndOffset
            }
        }
        .value
}
