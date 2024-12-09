package org.nekomanga.presentation.components.dropdown

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import kotlinx.collections.immutable.ImmutableList
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun SimpleDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    dropDownItems: ImmutableList<SimpleDropDownItem>,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    val background =
        Modifier.background(
            color =
                MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
                    themeColorState.buttonColor,
                    8.dp,
                )
        )
    CascadeDropdownMenu(
        expanded = expanded,
        offset = DpOffset(Size.small, Size.small),
        fixedWidth = 225.dp,
        modifier = background,
        properties = PopupProperties(),
        onDismissRequest = onDismiss,
    ) {
        val enabledStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-.5).sp,
            )
        val disabledStyle =
            enabledStyle.copy(enabledStyle.color.copy(alpha = NekoColors.disabledAlphaLowContrast))

        dropDownItems.forEach { item ->
            Row(
                modifier = background,
                item = item,
                enabledStyle = enabledStyle,
                disabledStyle = disabledStyle,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun CascadeColumnScope.Row(
    modifier: Modifier,
    item: SimpleDropDownItem,
    enabledStyle: TextStyle,
    disabledStyle: TextStyle,
    onDismiss: () -> Unit,
) {

    when (item) {
        is SimpleDropDownItem.Parent -> {

            val style =
                when (item.enabled) {
                    true -> enabledStyle
                    false -> disabledStyle
                }

            DropdownMenuItem(
                modifier = modifier,
                text = { Text(text = item.text.asString(), style = style) },
                enabled = item.enabled,
                children = {
                    for (child in item.children) {
                        Row(
                            modifier = modifier,
                            item = child,
                            enabledStyle = enabledStyle,
                            disabledStyle = disabledStyle,
                            onDismiss = onDismiss,
                        )
                    }
                },
                childrenHeader = {
                    DropdownMenuHeader(
                        modifier = modifier,
                        text = { Text(text = item.text.asString(), style = style) },
                    )
                },
            )
        }
        is SimpleDropDownItem.Action -> {
            val style =
                when (item.enabled) {
                    true -> enabledStyle
                    false -> disabledStyle
                }
            Item(
                modifier = modifier,
                text = item.text.asString(),
                style = style,
                enabled = item.enabled,
                onClick = item.onClick,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun Item(
    modifier: Modifier,
    text: String,
    style: TextStyle,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    MaterialDropdownMenuItem(
        modifier = modifier,
        enabled = enabled,
        text = { Text(text = text, style = style) },
        onClick = {
            onDismiss()
            onClick()
        },
    )
}

@Immutable
sealed class SimpleDropDownItem {
    data class Action(val text: UiText, val enabled: Boolean = true, val onClick: () -> Unit) :
        SimpleDropDownItem()

    data class Parent(
        val text: UiText,
        val enabled: Boolean = true,
        val children: List<SimpleDropDownItem>,
    ) : SimpleDropDownItem()
}
