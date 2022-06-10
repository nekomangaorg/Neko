package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.theme.Shapes

@Composable
fun EditCategorySheet() {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Shapes.sheetRadius)) {
        LazyColumn(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            item {
                Text(text = stringResource(id = R.string.move_x_to, stringResource(id = R.string.manga)), style = MaterialTheme.typography.titleLarge)
            }
            item {
                Gap(16.dp)
                Divider()
                Gap(16.dp)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var state = remember { mutableStateOf(true) }
                    Checkbox(
                        checked = state.value,
                        onCheckedChange = { newValue ->
                            state.value = newValue
                        },
                    )
                    Gap(4.dp)
                    Text("Text")
                }
            }
        }

    }
}
