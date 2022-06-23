package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chargemap.compose.numberpicker.ListItemPicker
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun TrackingScoreDialog(themeColors: ThemeColors, track: Track, service: TrackService, onDismiss: () -> Unit, trackScoreChange: (Int) -> Unit) {
    val scope = rememberCoroutineScope()

    val displayedScore = service.displayScore(track)
    val index = when {
        displayedScore == "-" -> 0
        service.getScoreList().indexOf(displayedScore) != -1 -> service.getScoreList().indexOf(displayedScore)
        else -> 0
    }

    var currentIndex by remember { mutableStateOf(index) }


    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.score), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                ListItemPicker(
                    modifier = Modifier.fillMaxWidth(.4f),
                    value = service.getScoreList()[currentIndex],
                    onValueChange = { newScore ->
                        currentIndex = service.getScoreList().indexOf(newScore)
                    },
                    list = service.getScoreList(), dividersColor = themeColors.buttonColor,
                    textStyle = MaterialTheme.typography.titleMedium,
                )
            }

        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColors.buttonColor)) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    trackScoreChange(currentIndex)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = themeColors.buttonColor),
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
    )
}
