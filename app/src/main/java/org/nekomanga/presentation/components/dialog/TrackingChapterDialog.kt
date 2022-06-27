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
import eu.kanade.tachiyomi.data.database.models.Track
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun TrackingChapterDialog(themeColors: ThemeColors, track: Track, onDismiss: () -> Unit, trackChapterChanged: (Int) -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColors.rippleTheme, LocalTextSelectionColors provides themeColors.textSelectionColors) {

        var currentChapter by remember { mutableStateOf(track.last_chapter_read.toInt()) }

        val range = when (track.total_chapters > 0) {
            true -> track.total_chapters
            false -> Int.MAX_VALUE
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
                        dividersColor = themeColors.buttonColor,
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
                        trackChapterChanged(currentChapter)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColors.buttonColor),
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
        )
    }
}
