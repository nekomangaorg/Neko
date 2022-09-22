package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import me.saket.cascade.DropdownMenuHeader
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun SimpleDropdownMenu(expanded: Boolean, onDismiss: () -> Unit, dropDownItems: ImmutableList<SimpleDropDownItem>, themeColorState: ThemeColorState? = null) {
    val customColor: Color = themeColorState?.buttonColor ?: MaterialTheme.colorScheme.surface
    val background = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(customColor, 8.dp))
    CascadeDropdownMenu(
        expanded = expanded,
        offset = DpOffset(8.dp, 0.dp),
        fixedWidth = 225.dp,
        modifier = background,
        onDismissRequest = onDismiss,
    ) {
        val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-.5).sp)
        dropDownItems.forEach { item ->
            Row(modifier = background, item = item, style = style, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun CascadeColumnScope.Row(modifier: Modifier, item: SimpleDropDownItem, style: TextStyle, onDismiss: () -> Unit) {
    when (item) {
        is SimpleDropDownItem.Parent -> {
            DropdownMenuItem(
                modifier = modifier,
                text = {
                    Text(
                        text = item.text,
                        style = style,
                    )
                },
                children = {
                    for (child in item.children) {
                        Row(modifier = modifier, item = child, style = style, onDismiss = onDismiss)
                    }
                },
                childrenHeader = {
                    DropdownMenuHeader(
                        modifier = modifier,
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
            Item(modifier = modifier, text = item.text, style = style, onClick = item.onClick, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun Item(modifier: Modifier, text: String, style: TextStyle, onClick: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.DropdownMenuItem(
        modifier = modifier,
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

@Immutable
sealed class SimpleDropDownItem {
    data class Action(val text: String, val onClick: () -> Unit) : SimpleDropDownItem()
    data class Parent(val text: String, val children: List<SimpleDropDownItem>) : SimpleDropDownItem()
}
