package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
fun DescriptionBlock(manga: Manga, isExpanded: Boolean, expandCollapseClick: () -> Unit = {}) {
    val description = when (MdUtil.getMangaId(manga.url).isDigitsOnly()) {
        true -> "THIS MANGA IS NOT MIGRATED TO V5"
        false -> manga.description ?: stringResource(R.string.no_description)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (isExpanded.not()) {
            val text = description.trim()
            Box {
                Markdown(content = text, modifier = Modifier.height(90.dp), colors = markdownColors(), typography = markdownTypography())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                            ),
                        ),
                ) {
                    Row(modifier = Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) {
                        Text(text = "Expand", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface))
                        Gap(4.dp)
                        Icon(imageVector = Icons.Filled.Expand, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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

