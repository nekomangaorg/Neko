package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
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
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import org.nekomanga.R
import org.nekomanga.presentation.components.ExpressivePicker
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun TrackingScoreDialog(
    themeColorState: ThemeColorState,
    trackAndService: TrackingConstants.TrackAndService,
    onDismiss: () -> Unit,
    trackScoreChange: (Int) -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        val displayedScore = trackAndService.service.displayScore(trackAndService.track)
        val index =
            when {
                displayedScore == "-" -> 0
                trackAndService.service.scoreList.indexOf(displayedScore) != -1 ->
                    trackAndService.service.scoreList.indexOf(displayedScore)
                else -> 0
            }

        var currentIndex by remember { mutableStateOf(index) }

        AlertDialog(
            title = {
                Text(
                    text = stringResource(id = R.string.score),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(Size.medium).fillMaxWidth(),
                ) {
                    ExpressivePicker(
                        modifier = Modifier.fillMaxWidth(.4f),
                        themeColorState = themeColorState,
                        items = trackAndService.service.scoreList,
                        value = trackAndService.service.scoreList[currentIndex],
                        onValueChange = { newScore ->
                            currentIndex = trackAndService.service.scoreList.indexOf(newScore)
                        },
                    )
                }
            },
            onDismissRequest = onDismiss,
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        trackScoreChange(currentIndex)
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor),
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
        )
    }
}
