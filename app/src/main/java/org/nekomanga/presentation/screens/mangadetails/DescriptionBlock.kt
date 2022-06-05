package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.mikepenz.markdown.Markdown
import com.mikepenz.markdown.MarkdownColors
import com.mikepenz.markdown.MarkdownDefaults
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import jp.wasabeef.gap.Gap
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.nekomanga.presentation.components.NekoColors

@Composable
fun DescriptionBlock(manga: Manga, buttonColor: Color, isExpanded: Boolean, expandCollapseClick: () -> Unit = {}) {
    val description = when (MdUtil.getMangaId(manga.url).isDigitsOnly()) {
        true -> "THIS MANGA IS NOT MIGRATED TO V5"
        false -> manga.description ?: stringResource(R.string.no_description)
    }

    Column(modifier = Modifier
        .padding(horizontal = 16.dp)
        .clickable { expandCollapseClick() }) {
        if (isExpanded.not()) {
            val text = description.replace(
                Regex(
                    "[\\r\\n\\s*]{2,}",
                    setOf(RegexOption.MULTILINE),
                ),
                "\n",
            )

            val lineHeight = with(LocalDensity.current) {
                MaterialTheme.typography.bodyLarge.fontSize.toDp() + 5.dp
            }

            Box {
                Text(
                    modifier = Modifier.align(Alignment.TopStart),
                    text = text,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)),
                )
                Box(
                    modifier = Modifier
                        .height(lineHeight)
                        .align(Alignment.BottomEnd)
                        .width(150.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = .8f), MaterialTheme.colorScheme.surface),
                            ),
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd),
                    ) {
                        Text(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(start = 8.dp),
                            text = "More",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = buttonColor,
                            ),
                        )
                        Gap(4.dp)
                        Icon(modifier = Modifier.align(Alignment.CenterVertically), imageVector = Icons.Filled.ExpandMore, contentDescription = null, tint = buttonColor)
                    }
                }
            }
        } else {
            val text = description.trim()
            Markdown(content = text, colors = markdownColors(), typography = markdownTypography(), flavour = CommonMarkFlavourDescriptor())
        }
    }
}

@Composable
private fun markdownColors(): MarkdownColors {
    return MarkdownDefaults.markdownColors(
        textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        backgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun markdownTypography() =
    MarkdownDefaults.markdownTypography(
        h1 = MaterialTheme.typography.headlineMedium,
        h2 = MaterialTheme.typography.headlineSmall,
        h3 = MaterialTheme.typography.titleLarge,
        h4 = MaterialTheme.typography.titleMedium,
        h5 = MaterialTheme.typography.titleSmall,
        h6 = MaterialTheme.typography.bodyLarge,
        body1 = MaterialTheme.typography.bodyLarge,
        body2 = MaterialTheme.typography.bodySmall,
    )

private fun Int.textDp(density: Density): TextUnit = with(density) {
    this@textDp.dp.toSp()
}

val Int.textDp: TextUnit
    @Composable get() = this.textDp(density = LocalDensity.current)
