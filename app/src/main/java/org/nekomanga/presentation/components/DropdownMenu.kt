package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.presentation.extensions.surfaceColorAtElevation

@Composable
fun SimpleDropdownMenu(expanded: Boolean, onDismiss: () -> Unit, dropDownItems: List<SimpleDropdownItem>) {
    CascadeDropdownMenu(
        expanded = expanded,
        offset = DpOffset(8.dp, 0.dp),
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
        onDismissRequest = onDismiss,
    ) {
        val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-.5).sp)
        dropDownItems.forEach { item ->
            androidx.compose.material3.DropdownMenuItem(
                modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
                text = {
                    Text(
                        text = item.text,
                        style = style,
                    )
                },
                onClick = {
                    item.onClick()
                    onDismiss()
                },
            )
        }
    }
}

data class SimpleDropdownItem(val text: String, val onClick: () -> Unit)
