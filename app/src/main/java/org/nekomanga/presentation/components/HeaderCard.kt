package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.nekomanga.R
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun HeaderCard(headerText: @Composable () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(Shapes.coverRadius),
        modifier = Modifier.fillMaxWidth().padding(Size.small),
        colors =
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) {
        headerText()
    }
}

@Composable
fun DefaultHeaderText(text: String, isExpanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Size.smedium)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.smedium),
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription =
                stringResource(id = if (isExpanded) R.string.collapse else R.string.expand),
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = Size.smedium),
        )
    }
}
