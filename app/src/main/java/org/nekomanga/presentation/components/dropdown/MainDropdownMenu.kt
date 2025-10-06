package org.nekomanga.presentation.components.dropdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.ColorUtils
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.R
import org.nekomanga.presentation.components.Divider
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiIcon
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun MainDropdownMenu(
    expanded: Boolean,
    incognitoModeEnabled: Boolean,
    incognitoModeClick: () -> Unit,
    settingsClick: () -> Unit,
    statsClick: () -> Unit,
    aboutClick: () -> Unit,
    helpClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val secondary = MaterialTheme.colorScheme.secondary
    val backgroundArgb = remember {
        ColorUtils.blendARGB(background.toArgb(), secondary.toArgb(), 0.05f)
    }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(surface = MaterialTheme.colorScheme.surface),
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp)),
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides (defaultThemeColorState().rippleConfiguration)
        ) {
            CascadeDropdownMenu(
                expanded = expanded,
                offset = DpOffset(12.dp, Size.none),
                fixedWidth = 250.dp,
                properties = PopupProperties(),
                onDismissRequest = onDismiss,
            ) {
                val (incognitoText, incognitoIcon) =
                    when (incognitoModeEnabled) {
                        true ->
                            R.string.turn_off_incognito_mode to
                                CommunityMaterial.Icon2.cmd_incognito_off
                        false ->
                            R.string.turn_on_incognito_mode to CommunityMaterial.Icon2.cmd_incognito
                    }

                Row(
                    title = UiText.StringResource(incognitoText),
                    subTitle = UiText.StringResource(R.string.pauses_reading_history),
                    icon = UiIcon.IIcon(incognitoIcon),
                    onClick = incognitoModeClick,
                    onDismiss = onDismiss,
                )
                Divider()
                Row(
                    title = UiText.StringResource(R.string.settings),
                    icon = UiIcon.Icon(Icons.Outlined.Settings),
                    onClick = settingsClick,
                    onDismiss = onDismiss,
                )
                Row(
                    title = UiText.StringResource(R.string.stats),
                    icon = UiIcon.Icon(Icons.Outlined.QueryStats),
                    onClick = statsClick,
                    onDismiss = onDismiss,
                )
                Row(
                    title = UiText.StringResource(R.string.about),
                    icon = UiIcon.Icon(Icons.Outlined.Info),
                    onClick = aboutClick,
                    onDismiss = onDismiss,
                )
                Row(
                    title = UiText.StringResource(R.string.help),
                    icon = UiIcon.Icon(Icons.AutoMirrored.Outlined.HelpOutline),
                    onClick = helpClick,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun Row(
    title: UiText,
    subTitle: UiText? = null,
    icon: UiIcon,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
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
                        modifier = Modifier.size(24.dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                is UiIcon.IIcon ->
                    Image(
                        asset = icon.icon,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onSurface),
                    )
            }
        },
        onClick = {
            onClick()
            onDismiss()
        },
    )
}
