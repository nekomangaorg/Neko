package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun RemovedChaptersDialog(themeColorState: ThemeColorState, chapters: ImmutableList<ChapterItem>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        val context = LocalContext.current

        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.delete_removed_chapters))
            },
            text = {
                val chapterNames = chapters.map { it.chapter.name }
                Text(
                    text = context.resources.getQuantityString(
                        R.plurals.deleted_chapters,
                        chapters.size,
                        chapters.size,
                        if (chapters.size > 5) {
                            "${chapterNames.take(5 - 1).joinToString(", ")}, " +
                                context.resources.getQuantityString(
                                    R.plurals.notification_and_n_more,
                                    (chapterNames.size - (4 - 1)),
                                    (chapterNames.size - (4 - 1)),
                                )
                        } else {
                            chapterNames.joinToString(", ")
                        },
                    ),
                )
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.keep))
                }
            },
        )
    }
}
