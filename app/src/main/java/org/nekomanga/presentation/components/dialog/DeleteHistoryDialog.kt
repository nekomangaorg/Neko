package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.screens.ThemeColorState

/**
 * Simple Dialog to add a new category
 */
@Composable
fun DeleteHistoryDialog(themeColorState: ThemeColorState, onDismiss: () -> Unit, simpleChapter: SimpleChapter, onConfirm: (Boolean) -> Unit) {
    val context = LocalContext.current
    var removeEntireHistory by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.remove_chapter_history_question), style = MaterialTheme.typography.titleSmall)
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.this_will_remove_the_read_date,
                        ),
                    )
                    Text(
                        text = simpleChapter.name, fontStyle = FontStyle.Italic,
                    )
                    Text(
                        text = stringResource(
                            R.string.are_you_sure,
                        ),
                    )
                    Gap(2.dp)
                    CheckboxRow(
                        modifier = Modifier.fillMaxWidth(),
                        checkedState = removeEntireHistory,
                        checkedChange = { removeEntireHistory = !removeEntireHistory },
                        themeColorState = themeColorState,
                        rowText = stringResource(id = R.string.reset_all_chapters),
                    )
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(removeEntireHistory)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
