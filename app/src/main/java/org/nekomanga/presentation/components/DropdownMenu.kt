package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import me.saket.cascade.DropdownMenuHeader
import org.nekomanga.presentation.extensions.surfaceColorAtElevation

@Composable
fun SimpleDropdownMenu(expanded: Boolean, onDismiss: () -> Unit, dropDownItems: List<SimpleDropDownItem>) {
    CascadeDropdownMenu(
        expanded = expanded,
        offset = DpOffset(8.dp, 0.dp),
        fixedWidth = 225.dp,
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
        onDismissRequest = onDismiss,
    ) {
        val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-.5).sp)
        dropDownItems.forEach { item ->
            Row(item = item, style = style, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun CascadeColumnScope.Row(item: SimpleDropDownItem, style: TextStyle, onDismiss: () -> Unit) {
    when (item) {
        is SimpleDropDownItem.Parent -> {
            DropdownMenuItem(
                modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
                text = {
                    Text(
                        text = item.text,
                        style = style,
                    )
                },
                children = {
                    for (child in item.children) {
                        Row(item = child, style = style, onDismiss = onDismiss)
                    }
                },
                childrenHeader = {
                    DropdownMenuHeader(
                        modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
                        text = {
                            Text(
                                text = item.text,
                                style = style,
                            )
                        },
                    )
                },
            )
        }
        is SimpleDropDownItem.Action -> {
            Item(text = item.text, style = style, onClick = item.onClick, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun Item(text: String, style: TextStyle, onClick: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.DropdownMenuItem(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
        text = {
            Text(
                text = text,
                style = style,
            )
        },
        onClick = {
            onClick()
            onDismiss()
        },
    )
}

sealed class SimpleDropDownItem {
    data class Action(val text: String, val onClick: () -> Unit) : SimpleDropDownItem()
    data class Parent(val text: String, val children: List<Action>) : SimpleDropDownItem()
}
