package org.nekomanga.presentation.components.dropdown

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import kotlinx.collections.immutable.PersistentList
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun SimpleDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    dropDownItems: PersistentList<SimpleDropDownItem>,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CascadeDropdownMenu(
        expanded = expanded,
        offset = DpOffset(Size.smedium, Size.none),
        fixedWidth = 250.dp,
        modifier =
            Modifier.background(
                color = themeColorState.containerColor.copy(alpha = NekoColors.highAlphaLowContrast)
            ),
        properties = PopupProperties(),
        shape = RoundedCornerShape(Size.medium),
        onDismissRequest = onDismiss,
    ) {
        val enabledStyle =
            MaterialTheme.typography.bodyLarge.copy(color = themeColorState.onContainerColor)
        val disabledStyle =
            enabledStyle.copy(enabledStyle.color.copy(alpha = NekoColors.disabledAlphaLowContrast))

        dropDownItems.forEach { item ->
            Row(
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
                text = { Text(text = item.text.asString(), style = style) },
                enabled = item.enabled,
                children = {
                    for (child in item.children) {
                        Row(
                            item = child,
                            enabledStyle = enabledStyle,
                            disabledStyle = disabledStyle,
                            onDismiss = onDismiss,
                        )
                    }
                },
                childrenHeader = {
                    DropdownMenuHeader(text = { Text(text = item.text.asString(), style = style) })
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
    text: String,
    style: TextStyle,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    MaterialDropdownMenuItem(
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
