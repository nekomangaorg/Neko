package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chargemap.compose.numberpicker.ListItemPicker
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingScoreDialog(themeColorState: ThemeColorState, trackAndService: TrackingConstants.TrackAndService, onDismiss: () -> Unit, trackScoreChange: (Int) -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        val displayedScore = trackAndService.service.displayScore(trackAndService.track)
        val index = when {
            displayedScore == "-" -> 0
            trackAndService.service.scoreList.indexOf(displayedScore) != -1 -> trackAndService.service.scoreList.indexOf(displayedScore)
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
                        value = trackAndService.service.scoreList[currentIndex],
                        onValueChange = { newScore ->
                            currentIndex = trackAndService.service.scoreList.indexOf(newScore)
                        },
                        list = trackAndService.service.scoreList,
                        dividersColor = themeColorState.buttonColor,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            },
            onDismissRequest = onDismiss,
            dismissButton = {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        trackScoreChange(currentIndex)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
        )
    }
}
