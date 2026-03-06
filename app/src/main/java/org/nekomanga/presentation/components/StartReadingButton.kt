package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.nekomanga.R
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size
import org.nekomanga.ui.theme.ThemeConfig
import org.nekomanga.ui.theme.ThemeConfigProvider
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
fun StartReadingButton(modifier: Modifier = Modifier, onStartReadingClick: () -> Unit) {
    val contentDescriptionString = stringResource(id = R.string.start_reading)
    Box(
        modifier =
            modifier
                .padding(Size.extraTiny)
                .clip(
                    shape =
                        RoundedCornerShape(
                            topStart = Size.tiny,
                            bottomStart = Size.tiny,
                            bottomEnd = Size.tiny,
                            topEnd = Shapes.coverRadius,
                        )
                )
                .clickable(onClick = onStartReadingClick, role = Role.Button)
                .semantics { contentDescription = contentDescriptionString }
                .border(
                    width = Outline.thickness,
                    color = Outline.color,
                    shape =
                        RoundedCornerShape(
                            topStart = Size.tiny,
                            bottomStart = Size.tiny,
                            bottomEnd = Size.tiny,
                            topEnd = Shapes.coverRadius,
                        ),
                )
                .background(MaterialTheme.colorScheme.secondary)
                .size(Size.extraLarge)
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center).size(Size.large),
            imageVector = Icons.AutoMirrored.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@Preview
@Composable
private fun StartReadingButtonPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) { StartReadingButton(onStartReadingClick = {}) }
}
