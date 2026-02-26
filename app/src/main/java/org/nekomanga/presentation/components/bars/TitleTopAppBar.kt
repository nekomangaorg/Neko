package org.nekomanga.presentation.components.bars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.AutoSizeText
import org.nekomanga.presentation.components.FlexibleTopBar
import org.nekomanga.presentation.components.FlexibleTopBarColors
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.icons.IncognitoCircleIcon
import org.nekomanga.presentation.theme.Size

@Composable
fun TitleTopAppBar(
    color: Color,
    onColor: Color = LocalContentColor.current,
    title: String = "",
    subtitle: String = "",
    navigationIconLabel: String = "",
    navigationIcon: ImageVector? = null,
    incognitoMode: Boolean,
    onNavigationIconClicked: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    scrolledContainerColor: Color = Color.Transparent,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    FlexibleTopBar(
        scrollBehavior = scrollBehavior,
        colors =
            FlexibleTopBarColors(
                containerColor = color,
                scrolledContainerColor = scrolledContainerColor,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Size.small)
        ) {
            Column(modifier = Modifier.fillMaxWidth(.8f).align(Alignment.Center)) {
                if (title.isEmpty() && subtitle.isEmpty()) {
                    // Do nothing
                } else if (subtitle.isEmpty()) {
                    // center the text
                    AutoSizeText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(color = onColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AutoSizeText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(color = onColor),
                    )

                    AutoSizeText(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium.copy(color = onColor),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(Size.appBarHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                if (navigationIcon != null)
                    ToolTipButton(
                        toolTipLabel = navigationIconLabel,
                        icon = navigationIcon,
                        onClick = onNavigationIconClicked,
                        enabledTint = onColor,
                    )
                if (incognitoMode) {
                    Gap(Size.smedium)
                    Icon(
                        imageVector = IncognitoCircleIcon,
                        modifier = Modifier.size(Size.extraLarge),
                        contentDescription = null,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(Size.appBarHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                actions()
            }
        }
    }
}
