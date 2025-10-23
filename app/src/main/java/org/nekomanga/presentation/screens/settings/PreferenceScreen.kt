package org.nekomanga.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.delay
import org.nekomanga.presentation.screens.settings.Preference.PreferenceItem
import org.nekomanga.presentation.screens.settings.screens.SearchableSettings
import org.nekomanga.presentation.screens.settings.widgets.PreferenceGroupHeader

/**
 * Preference Screen composable which contains a list of [Preference] items
 *
 * @param items [Preference] items which should be displayed on the preference screen. An item can
 *   be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: PersistentList<Preference>,
    incomingHighlightKey: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    LazyColumn(modifier = modifier, state = state, contentPadding = contentPadding) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item(key = preference.title) {
                        Column { PreferenceGroupHeader(title = preference.title) }
                    }
                    items(
                        items = preference.preferenceItems,
                        key = { item -> "${preference.title}-${item.title}" },
                    ) { item ->
                        PreferenceItem(item = item, highlightKey = highlightKey)
                    }
                    item(key = "spacer-$i") {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is PreferenceItem<*> ->
                    item(key = preference.title) {
                        PreferenceItem(item = preference, highlightKey = highlightKey)
                    }
            }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
            if (it is Preference.PreferenceGroup) {
                buildList<String?> {
                    add(null) // Header
                    addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                    add(null) // Spacer
                }
            } else {
                listOf(it.title)
            }
        }
        .indexOfFirst { it == highlightKey }
}
