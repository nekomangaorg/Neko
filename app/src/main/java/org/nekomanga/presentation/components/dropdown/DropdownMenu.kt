package org.nekomanga.presentation.components.dropdown

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Immutable
data class DropdownMenuItem(
    val title: UiText,
    val icon: ImageVector,
    val subtitle: UiText? = null,
    val onClick: () -> Unit,
)

@Composable
fun NekoDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    themeColorState: ThemeColorState,
    modifier: Modifier = Modifier,
    content: @Composable CascadeColumnScope.() -> Unit,
) {
    val colors =
        MaterialTheme.colorScheme.copy(
            primary = themeColorState.primaryColor,
            surface = themeColorState.altContainerColor,
            surfaceContainer = themeColorState.altContainerColor,
            onSurface = themeColorState.onAltContainerColor,
            onSurfaceVariant = themeColorState.onAltContainerColor,
        )

    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides (themeColorState.rippleConfiguration)
        ) {
            CascadeDropdownMenu(
                expanded = expanded,
                offset = DpOffset(Size.smedium, Size.none),
                fixedWidth = 250.dp,
                modifier = modifier.background(color = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(Size.medium),
                properties = PopupProperties(),
                onDismissRequest = onDismissRequest,
                content = content,
            )
        }
    }
}
