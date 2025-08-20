package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.settings.BasePreferenceWidget
import org.nekomanga.presentation.theme.Size

@Composable
fun TrackingPreferenceWidget(
    modifier: Modifier = Modifier,
    tracker: TrackServiceItem,
    title: String,
    subtitle: String? = null,
    loggedIn: Boolean,
    onClick: (() -> Unit)? = null,
) {

    BasePreferenceWidget(
        modifier = modifier,
        title = title,
        subcomponent =
            if (!subtitle.isNullOrBlank()) {
                {
                    Text(
                        text = subtitle,
                        modifier =
                            Modifier.padding(horizontal = Size.medium)
                                .alpha(NekoColors.mediumAlphaLowContrast),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 10,
                    )
                }
            } else {
                null
            },
        icon = {
            Box(
                modifier =
                    Modifier.size(Size.huge)
                        .clip(RoundedCornerShape(Size.small))
                        .background(color = Color(tracker.logoColor))
            ) {
                Image(
                    painter = painterResource(id = tracker.logoRes),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        },
        onClick = { onClick?.invoke() },
        widget = {
            Icon(
                modifier = Modifier.size(Size.extraLarge),
                imageVector =
                    if (loggedIn) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                tint =
                    if (loggedIn) MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(
                            NekoColors.disabledAlphaHighContrast
                        ),
                contentDescription = null,
            )
        },
    )
}

@Composable
fun SitePreferenceWidget(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    loggedIn: Boolean,
    onClick: () -> Unit,
) {
    TextPreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        onPreferenceClick = onClick,
        widget = {
            Icon(
                imageVector =
                    if (loggedIn) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                tint =
                    if (loggedIn) MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(
                            NekoColors.disabledAlphaHighContrast
                        ),
                contentDescription = null,
            )
        },
    )
}
