package eu.kanade.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.theme.Padding.horizontalPadding

@Composable
fun Divider() {
    androidx.compose.material3.Divider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    )
}

@Composable
fun PreferenceRow(
    title: String,
    painter: Painter? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    val height = if (subtitle != null) 72.dp else 56.dp

    val titleTextStyle = MaterialTheme.typography.bodyLarge
    val subtitleTextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
        }
        Column(
            Modifier
                .padding(horizontal = horizontalPadding)
                .weight(1f),
        ) {
            Text(
                text = title,
                style = titleTextStyle,
            )
            if (subtitle != null) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = subtitle,
                    style = subtitleTextStyle,
                )
            }
        }
        if (action != null) {
            Box(Modifier.widthIn(min = 56.dp)) {
                action()
            }
        }
    }
}

/*@Composable
fun SwitchPreference(
    preference: PreferenceMutableState<Boolean>,
    title: String,
    subtitle: String? = null,
    painter: Painter? = null,
) {
    PreferenceRow(
        title = title,
        subtitle = subtitle,
        painter = painter,
        action = {
            Switch(checked = preference.value, onCheckedChange = null)
            // TODO: remove this once switch checked state is fixed: https://issuetracker.google.com/issues/228336571
            Text(preference.value.toString())
        },
        onClick = { preference.value = !preference.value },
    )
}*/
