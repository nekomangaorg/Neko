package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.nekomanga.R
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Size

@Composable
fun InLibraryIcon(offset: Dp, outline: Boolean) {
    val inLibraryDescription = stringResource(id = R.string.in_library)
    Box(
        modifier =
            Modifier.offset(x = offset, y = offset).semantics {
                contentDescription = inLibraryDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        if (outline) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = Outline.color,
                modifier = Modifier.size(21.5.dp),
            )
        }
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun InLibraryBadge(outline: Boolean, offset: Dp = (-2).dp) {
    Box(
        modifier =
            Modifier.offset(x = offset, y = offset)
                .clip(
                    RoundedCornerShape(
                        topStartPercent = 50,
                        25,
                        bottomStartPercent = 25,
                        bottomEndPercent = 50,
                    )
                )
                .background(color = MaterialTheme.colorScheme.secondary)
                .conditional(outline) {
                    this.border(
                        width = Outline.thickness,
                        color = Outline.color,
                        shape =
                            RoundedCornerShape(
                                topStartPercent = 50,
                                25,
                                bottomStartPercent = 25,
                                bottomEndPercent = 50,
                            ),
                    )
                }
    ) {
        AutoSizeText(
            text = stringResource(id = R.string.in_library),
            style =
                MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSecondary
                ),
            modifier = Modifier.padding(horizontal = Size.tiny, vertical = Size.extraTiny),
        )
    }
}
