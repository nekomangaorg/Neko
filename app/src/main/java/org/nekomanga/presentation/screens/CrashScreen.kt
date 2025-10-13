package org.nekomanga.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import eu.kanade.tachiyomi.util.CrashLogUtil
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

@Composable
fun CrashScreen(exception: Throwable?, onRestartClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val systemUiController = rememberSystemUiController()
    val color = MaterialTheme.colorScheme.surface
    val useDarkIcons = color.luminance() > .5f
    DisposableEffect(color, useDarkIcons) {
        systemUiController.setStatusBarColor(color, darkIcons = useDarkIcons)
        onDispose {}
    }

    Scaffold(
        bottomBar = {
            val strokeWidth = Dp.Hairline
            val borderColor = MaterialTheme.colorScheme.outline
            Column(
                modifier =
                    Modifier.drawBehind {
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
                    onClick = { scope.launch { CrashLogUtil(context).dumpLogs(exception) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.pref_dump_crash_logs))
                }
                OutlinedButton(onClick = onRestartClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.crash_screen_restart_application))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(paddingValues)
                    .padding(horizontal = Size.medium)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
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
            Gap(paddingValues.calculateBottomPadding())
        }
    }
}
