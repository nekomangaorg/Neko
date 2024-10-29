package org.nekomanga.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FooterFilterChip(selected: Boolean, onClick: () -> Unit, name: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            if (selected) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
        },
        shape = RoundedCornerShape(100),
        label = {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            )
        },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                labelColor = MaterialTheme.colorScheme.secondary,
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.secondary,
                selectedBorderColor = Color.Transparent,
                borderWidth = 2.dp,
            ),
    )
}
