package org.nekomanga.presentation.components.dropdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import me.saket.cascade.CascadeColumnScope
import org.nekomanga.R
import org.nekomanga.presentation.components.Divider
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiIcon
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun MainDropdownMenu(
    themeColorState: ThemeColorState = defaultThemeColorState(),
    expanded: Boolean,
    incognitoModeEnabled: Boolean,
    incognitoModeClick: () -> Unit,
    settingsClick: () -> Unit,
    statsClick: () -> Unit,
    aboutClick: () -> Unit,
    helpClick: () -> Unit,
    onDismiss: () -> Unit,
) {

    val menuItems =
        remember(incognitoModeEnabled) {
            val (incognitoText, incognitoIcon) =
                if (incognitoModeEnabled) {
                    R.string.turn_off_incognito_mode to CommunityMaterial.Icon2.cmd_incognito_off
                } else {
                    R.string.turn_on_incognito_mode to CommunityMaterial.Icon2.cmd_incognito
                }

            listOf(
                DropdownMenuItem(
                    title = UiText.StringResource(incognitoText),
                    subtitle = UiText.StringResource(R.string.pauses_reading_history),
                    icon = UiIcon.IIcon(incognitoIcon),
                    onClick = incognitoModeClick,
                ),
                DropdownMenuItem(
                    title = UiText.StringResource(R.string.settings),
                    icon = UiIcon.Icon(Icons.Outlined.Settings),
                    onClick = settingsClick,
                ),
                DropdownMenuItem(
                    title = UiText.StringResource(R.string.stats),
                    icon = UiIcon.Icon(Icons.Outlined.QueryStats),
                    onClick = statsClick,
                ),
                DropdownMenuItem(
                    title = UiText.StringResource(R.string.about),
                    icon = UiIcon.Icon(Icons.Outlined.Info),
                    onClick = aboutClick,
                ),
                DropdownMenuItem(
                    title = UiText.StringResource(R.string.help),
                    icon = UiIcon.Icon(Icons.AutoMirrored.Outlined.HelpOutline),
                    onClick = helpClick,
                ),
            )
        }

    NekoDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        themeColorState = themeColorState,
    ) {
        menuItems.forEachIndexed { index, item ->
            Row(
                title = item.title,
                subTitle = item.subtitle,
                icon = item.icon,
                onClick = {
                    item.onClick()
                    onDismiss()
                },
            )
            if (index == 0) {
                Divider()
            }
        }
    }
}

@Composable
private fun CascadeColumnScope.Row(
    title: UiText,
    subTitle: UiText? = null,
    icon: UiIcon,
    onClick: () -> Unit,
) {
    MaterialDropdownMenuItem(
        text = {
            Column {
                Text(
                    text = title.asString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (subTitle != null) {
                    Text(
                        text = subTitle.asString(),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                color =
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = NekoColors.mediumAlphaLowContrast
                                    )
                            ),
                    )
                }
            }
        },
        leadingIcon = {
            when (icon) {
                is UiIcon.Icon ->
                    Icon(
                        imageVector = icon.icon,
                        modifier = Modifier.size(Size.large),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                is UiIcon.IIcon ->
                    Image(
                        asset = icon.icon,
                        modifier = Modifier.size(Size.large),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onSurface),
                    )
            }
        },
        onClick = onClick,
    )
}
