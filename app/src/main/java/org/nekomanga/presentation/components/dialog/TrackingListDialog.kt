package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackList
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun TrackingListDialog(
    themeColorState: ThemeColorState,
    currentLists: ImmutableList<TrackList>,
    service: TrackServiceItem,
    onDismiss: () -> Unit,
    addToListClick: (String) -> Unit,
    removeFromListClick: (String) -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            title = {
                Text(text = stringResource(id = R.string.list), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    service.lists?.forEachIndexed { index, list ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = (currentLists.contains(list)),
                                onCheckedChange = { enabled ->
                                    when (enabled) {
                                        true -> removeFromListClick(list.id)
                                        false -> addToListClick(list.id)
                                    }
                                    onDismiss()

                                },
                                colors = CheckboxDefaults.colors(checkedColor = themeColorState.buttonColor),
                            )
                            Gap(width = 8.dp)
                            Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
