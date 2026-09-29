package org.nekomanga.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import org.nekomanga.R
import org.nekomanga.presentation.components.scaffold.RootScaffold
import org.nekomanga.presentation.theme.Size

@Composable
fun CrashScreen(
    exception: Throwable?,
    isSharing: Boolean,
    onShareClick: () -> Unit,
    onRestartClick: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

    RootScaffold(
        topBar = {},
        scrollBehavior = scrollBehavior,
        mainSettingsExpanded = false,
        bottomBar = {
            val strokeWidth = Dp.Hairline
            val borderColor = MaterialTheme.colorScheme.outline
            Column(
                modifier =
                    Modifier.navigationBarsPadding()
                        .drawBehind {
                            drawLine(
                                borderColor,
                                Offset(0f, 0f),
                                Offset(size.width, 0f),
                                strokeWidth.value,
                            )
                        }
                        .padding(horizontal = Size.medium, vertical = Size.small),
                verticalArrangement = Arrangement.spacedBy(Size.small),
            ) {
                Button(
                    onClick = onShareClick,
                    enabled = !isSharing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.share_crash_log))
                }
                OutlinedButton(onClick = onRestartClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.crash_screen_restart_application))
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.padding(contentPadding)
                    .padding(horizontal = Size.medium)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
                modifier = Modifier.size(Size.extraExtraHuge),
            )
            Text(
                text = stringResource(R.string.crash_screen_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text =
                    stringResource(
                        R.string.crash_screen_description,
                        stringResource(id = R.string.app_name),
                    ),
                modifier = Modifier.padding(horizontal = Size.medium),
            )
            Box(
                modifier =
                    Modifier.padding(vertical = Size.small)
                        .clip(MaterialTheme.shapes.small)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = exception.toString(),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    modifier = Modifier.padding(all = Size.small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
