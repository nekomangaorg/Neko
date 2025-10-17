package org.nekomanga.presentation.components.dropdown

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlinx.collections.immutable.PersistentList
import me.saket.cascade.CascadeColumnScope
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun SimpleDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    dropDownItems: PersistentList<SimpleDropDownItem>,
    themeColorState: ThemeColorState,
) {
    NekoDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        themeColorState = themeColorState,
    ) {
        val enabledStyle =
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
        val disabledStyle = enabledStyle.copy(color = enabledStyle.color.copy(alpha = .38f))

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
                modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
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
                    DropdownMenuHeader(
                        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
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
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
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
