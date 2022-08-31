package org.nekomanga.presentation.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.stats.StatsConstants

fun LazyListScope.detailedStats(detailedStats: State<StatsConstants.DetailedState>) {
    items(detailedStats.value.manga, key = { manga -> manga.id }) { manga ->
        DetailedCard(manga)
    }
}

@Composable
private fun DetailedCard(manga: StatsConstants.DetailedStatManga) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(text = manga.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Line(stringResource(id = R.string.typeNoSemi), stringResource(id = manga.type.typeRes))
                Line(stringResource(id = R.string.status), stringResource(id = manga.status.statusRes))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Line(stringResource(id = R.string.read_duration), manga.readDuration)
                Line(stringResource(id = R.string.start_year), manga.startYear?.toString() ?: stringResource(id = R.string.n_a))
            }
            Line(stringResource(id = R.string.read_chapter_count), manga.readChapters.toString() + " / " + manga.totalChapters.toString())
            Line(stringResource(id = R.string.categories), manga.categories.joinToString(", "))
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}





