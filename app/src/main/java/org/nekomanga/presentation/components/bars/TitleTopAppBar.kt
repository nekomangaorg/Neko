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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.AutoSizeText
import org.nekomanga.presentation.components.FlexibleTopBar
import org.nekomanga.presentation.components.FlexibleTopBarColors
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.theme.Size

@Composable
fun TitleTopAppBar(
    color: Color,
    onColor: Color,
    title: String = "",
    subtitle: String = "",
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    incognitoMode: Boolean,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    FlexibleTopBar(
        scrollBehavior = scrollBehavior,
        colors =
            FlexibleTopBarColors(containerColor = color, scrolledContainerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Size.small)
        ) {
            Column(modifier = Modifier.fillMaxWidth(.8f).align(Alignment.Center)) {
                if (title.isNotEmpty()) {
                    AutoSizeText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(color = onColor),
                    )
                }
                if (subtitle.isNotEmpty()) {
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
                ToolTipButton(
                    toolTipLabel = navigationIconLabel,
                    icon = navigationIcon,
                    onClick = onNavigationIconClicked,
                    enabledTint = onColor,
                )
                if (incognitoMode) {
                    Gap(Size.smedium)
                    Image(
                        CommunityMaterial.Icon2.cmd_incognito_circle,
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(Size.extraLarge).zIndex(1f),
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
