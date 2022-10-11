package org.nekomanga.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableRow(rowText: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = rowText)
            val icon = when (isExpanded) {
                true -> Icons.Default.ExpandLess
                false -> Icons.Default.ExpandMore
            }
            Icon(imageVector = icon, contentDescription = null)
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(animationSpec = tween(300)), exit = shrinkVertically(animationSpec = tween(300))) {
            Column(modifier = Modifier) {
                content()
            }
        }

    }
}
