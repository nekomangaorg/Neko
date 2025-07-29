package org.nekomanga.presentation.screens.settings.widgets

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun TrackingPreferenceWidget(
    modifier: Modifier = Modifier,
    tracker: TrackServiceItem,
    loggedIn: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Box {
        Row(
            modifier =
                modifier
                    .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                    .fillMaxWidth()
                    .padding(Size.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(56.dp)
                        .padding(start = 1.dp, top = 1.dp)
                        .clip(RoundedCornerShape(topStart = 15.dp))
                        .background(color = Color(tracker.logoColor))
            ) {
                Image(
                    painter = painterResource(id = tracker.logoRes),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Text(
                text = stringResource(tracker.nameRes),
                modifier = Modifier.weight(1f).padding(horizontal = Size.medium),
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
            )
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                tint =
                    if (loggedIn) MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(NekoColors.mediumAlphaLowContrast),
                contentDescription = null,
            )
        }
    }
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
                    if (loggedIn) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                tint =
                    if (loggedIn) MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(NekoColors.mediumAlphaLowContrast),
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun LoginPreferenceWidget(
    modifier: Modifier,
    @StringRes name: Int,
    showLogo: Boolean,
    @DrawableRes logoRes: Int,
    @ColorInt logoBackgroundColorInt: Int,
    loggedIn: Boolean,
    onClick: (() -> Unit)? = null,
) {

    Box {
        Row(
            modifier =
                modifier
                    .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                    .fillMaxWidth()
                    .padding(Size.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showLogo) {
                Box(
                    modifier =
                        Modifier.size(Size.extraHuge)
                            .background(color = Color(logoBackgroundColorInt))
                ) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            // Column {
            Text(
                text = stringResource(name),
                modifier = Modifier.weight(1f).padding(horizontal = Size.medium),
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
            )
            /*  Text(
                    text = stringResource(R.string.log_in),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Size.medium),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                )
            }*/

            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                tint =
                    if (loggedIn) MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(NekoColors.mediumAlphaLowContrast),
                contentDescription = null,
            )
        }
    }
}
