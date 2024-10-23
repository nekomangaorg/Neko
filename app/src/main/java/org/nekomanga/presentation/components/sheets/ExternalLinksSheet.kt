package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.external.ExternalLink
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun ExternalLinksSheet(
    themeColorState: ThemeColorState,
    externalLinks: List<ExternalLink>,
    onLinkClick: (String, String) -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {
        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
                horizontalArrangement = Arrangement.spacedBy(Size.small, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(Size.small),
            ) {
                externalLinks.forEach { LinkCard(externalLink = it, onLinkClick = onLinkClick) }
            }
        }
    }
}

@Composable
private fun LinkCard(externalLink: ExternalLink, onLinkClick: (String, String) -> Unit) {
    OutlinedCard(
        onClick = { onLinkClick(externalLink.getUrl(), externalLink.name) },
        modifier = Modifier.height(Size.huge),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(externalLink.logoColor)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
            if (externalLink.showLogo) {
                Gap(Size.small)
                Color.White
                Image(
                    painter = painterResource(id = externalLink.logo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).padding(top = Size.tiny, bottom = Size.tiny),
                )
                Gap(Size.small)
            } else {
                Gap(12.dp)
            }
            Text(
                text = externalLink.name,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = Color(externalLink.onLogoColor)
                    ),
            )
            Gap(12.dp)
        }
    }
}
