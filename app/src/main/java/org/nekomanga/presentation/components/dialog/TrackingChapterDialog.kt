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
import com.chargemap.compose.numberpicker.NumberPicker
import eu.kanade.tachiyomi.R
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingChapterDialog(themeColorState: ThemeColorState, track: TrackItem, onDismiss: () -> Unit, trackChapterChanged: (Int) -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        var currentChapter by remember { mutableStateOf(track.lastChapterRead.toInt()) }

        val range = when (track.totalChapters > 0) {
            true -> track.totalChapters
            false -> 10000
        }

        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.chapters), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    NumberPicker(
                        modifier = Modifier.fillMaxWidth(.4f),
                        value = currentChapter,
                        onValueChange = { newChapter ->
                            currentChapter = newChapter
                        },
                        range = 0..range,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        dividersColor = themeColorState.buttonColor,
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
                        trackChapterChanged(currentChapter)
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
