package org.nekomanga.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.screens.settings.Preference.PreferenceItem
import org.nekomanga.presentation.screens.settings.screens.SearchableSettings
import org.nekomanga.presentation.screens.settings.widgets.PreferenceGroupHeader
import org.nekomanga.presentation.theme.Size

/**
 * Preference Screen composable which contains a list of [Preference] items
 *
 * @param items [Preference] items which should be displayed on the preference screen. An item can
 *   be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
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

    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item(key = preference.title) {
                        Column { PreferenceGroupHeader(title = preference.title) }
                    }
                    itemsIndexed(
                        items = preference.preferenceItems,
                        key = { _, item -> "${preference.title}-${item.title}" },
                    ) { index, item ->
                        val cardType = when {
                            preference.preferenceItems.size == 1 -> ListCardType.Single
                            index == 0 -> ListCardType.Top
                            index == preference.preferenceItems.lastIndex -> ListCardType.Bottom
                            else -> ListCardType.Center
                        }
                        ExpressiveListCard(
                            listCardType = cardType,
                            modifier = Modifier.padding(horizontal = Size.medium),
                        ) {
                            PreferenceItem(item = item, highlightKey = highlightKey)
                        }
                    }
                }

                // Create Preference Item
                is PreferenceItem<*> ->
                    item(key = preference.title) {
                        ExpressiveListCard(
                            listCardType = ListCardType.Single,
                            modifier = Modifier.padding(horizontal = Size.medium),
                        ) {
                            PreferenceItem(item = preference, highlightKey = highlightKey)
                        }
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
