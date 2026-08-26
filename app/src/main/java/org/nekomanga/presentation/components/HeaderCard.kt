package org.nekomanga.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import org.nekomanga.R
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun HeaderCard(modifier: Modifier = Modifier, headerText: @Composable () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(Shapes.coverRadius),
        modifier = modifier.fillMaxWidth().padding(Size.small),
        colors =
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) {
        headerText()
    }
}

@Composable
fun DefaultHeaderText(
    text: String,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val stateDescription =
        stringResource(id = if (isExpanded) R.string.expanded else R.string.collapsed)
    val chevronRotation by
        animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            label = "header_chevron_rotation",
        )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    this.stateDescription = stateDescription
                }
                .clickable(onClick = onClick)
                .padding(vertical = Size.smedium)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.huge),
        )
        Icon(
            imageVector = Icons.Default.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .padding(end = Size.smedium)
                    .rotate(chevronRotation),
        )
    }
}
