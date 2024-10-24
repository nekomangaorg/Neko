package org.nekomanga.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import org.nekomanga.presentation.components.LauncherIcon
import org.nekomanga.presentation.theme.Size

@Composable
fun InfoScreen(
    icon: ImageVector? = null,
    headingText: String,
    subtitleText: String,
    acceptText: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onAcceptClick: () -> Unit,
    canAccept: Boolean = true,
    rejectText: String? = null,
    onRejectClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        bottomBar = {
            val strokeWidth = Dp.Hairline
            val borderColor = MaterialTheme.colorScheme.outline
            Column(
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.background)
                        .drawBehind {
                            drawLine(
                                borderColor,
                                Offset(0f, 0f),
                                Offset(size.width, 0f),
                                strokeWidth.value,
                            )
                        }
                        .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                        .padding(horizontal = Size.medium, vertical = Size.small)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = tint),
                    onClick = onAcceptClick,
                ) {
                    Text(text = acceptText)
                }
                if (rejectText != null && onRejectClick != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(width = Size.extraExtraTiny, color = tint),
                        onClick = onRejectClick,
                    ) {
                        Text(text = rejectText)
                    }
                }
            }
        }
    ) { paddingValues ->
        // Status bar scrim
        Box(
            modifier =
                Modifier.zIndex(2f)
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
                    .height(paddingValues.calculateTopPadding())
        )

        Column(
            modifier =
                Modifier.verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(top = Size.large)
                    .padding(horizontal = Size.medium)
        ) {
            when (icon != null) {
                true -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = Size.small).size(Size.huge),
                        tint = tint,
                    )
                }
                false -> {
                    Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        LauncherIcon(Size.extraHuge)
                    }
                }
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = headingText,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitleText,
                modifier = Modifier.padding(vertical = Size.small),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )

            content()
        }
    }
}

@Preview
@Composable
private fun InfoScaffoldPreview() {
    InfoScreen(
        icon = Icons.Outlined.Newspaper,
        headingText = "Heading",
        subtitleText = "Subtitle",
        acceptText = "Accept",
        onAcceptClick = {},
        rejectText = "Reject",
        onRejectClick = {},
    ) {
        Text("Hello world")
    }
}
