package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.Image
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
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import eu.kanade.tachiyomi.data.external.ExternalLink
import jp.wasabeef.gap.Gap
import onColor
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun ExternalLinksSheet(themeColors: ThemeColors, externalLinks: List<ExternalLink>, onLinkClick: (String) -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColors.rippleTheme) {

        BaseSheet(themeColors = themeColors) {
            Gap(8.dp)
            FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisAlignment = FlowMainAxisAlignment.Start, crossAxisSpacing = 8.dp, mainAxisSpacing = 8.dp) {
                externalLinks.forEach {
                    LinkCard(externalLink = it, onLinkClick = onLinkClick)
                }
            }
        }
    }
}

@Composable
private fun LinkCard(externalLink: ExternalLink, onLinkClick: (String) -> Unit) {
    OutlinedCard(
        onClick = { onLinkClick(externalLink.getUrl()) },
        modifier = Modifier.height(48.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(externalLink.getLogoColor())),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {

            if (externalLink.isOther().not()) {
                Gap(8.dp)
                Image(
                    painter = painterResource(id = externalLink.getLogo()), contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                )
                Gap(8.dp)
            } else {
                Gap(12.dp)
            }
            Text(
                text = externalLink.name,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(externalLink.getLogoColor()).onColor()),
            )
            Gap(12.dp)
        }
    }
}
