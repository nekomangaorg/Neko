package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun TrackingStatusDialog(themeColors: ThemeColors, initialStatus: Int, service: TrackService, onDismiss: () -> Unit, trackStatusChange: (Int) -> Unit) {
    var selectedStatus by remember { mutableStateOf(initialStatus) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.status), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                service.getStatusList().forEach { status ->

                    val clicked = {
                        selectedStatus = status
                        scope.launch {
                            delay(100L)
                            trackStatusChange(selectedStatus)
                            onDismiss()
                        }
                    }


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (status == selectedStatus),
                                onClick = {
                                    clicked()
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        RadioButton(
                            selected = (status == selectedStatus),
                            onClick = {
                                clicked()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = themeColors.buttonColor),
                        )
                        Gap(width = 8.dp)
                        Text(text = service.getStatus(status), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColors.buttonColor)) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}
