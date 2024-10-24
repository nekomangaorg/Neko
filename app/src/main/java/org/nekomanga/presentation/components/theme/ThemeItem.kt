package org.nekomanga.presentation.components.theme

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material3.createMdc3Theme
import eu.kanade.tachiyomi.util.system.Themes
import eu.kanade.tachiyomi.util.system.isInNightMode
import org.nekomanga.R
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun ThemeItem(theme: Themes, isDarkTheme: Boolean, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val selectedColor = MaterialTheme.colorScheme.primary
    val configuration = Configuration(context.resources.configuration)
    configuration.uiMode =
        if (isDarkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    val themeContext = context.createConfigurationContext(configuration)
    themeContext.setTheme(theme.styleRes)
    val colorScheme =
        createMdc3Theme(
                context = themeContext,
                layoutDirection = LayoutDirection.Ltr,
                setTextColors = true,
                readTypography = false,
            )
            .colorScheme!!

    val themeMatchesApp =
        if (context.isInNightMode()) {
            isDarkTheme
        } else {
            !isDarkTheme
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
        AppThemePreviewItem(
            colorScheme,
            selected,
            selectedColor = MaterialTheme.colorScheme.primary,
            themeMatchesApp,
            onClick,
        )
        Text(
            text = stringResource(id = if (isDarkTheme) theme.darkNameRes else theme.nameRes),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun AppThemePreviewItem(
    colorScheme: ColorScheme,
    selected: Boolean,
    selectedColor: Color,
    themeMatchesApp: Boolean,
    onClick: () -> Unit,
) {

    val selectedColor =
        when {
            themeMatchesApp && selected -> colorScheme.primary
            selected -> selectedColor.copy(alpha = NekoColors.halfAlpha)
            else -> Color.Transparent
        }

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.height(180.dp).fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.background),
        border = BorderStroke(width = Size.tiny, color = selectedColor),
    ) {
        // App Bar
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(Size.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxHeight(0.8f)
                        .weight(0.7f)
                        .padding(end = Size.small)
                        .background(
                            color = colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        )
            )

            Box(modifier = Modifier.weight(0.3f), contentAlignment = Alignment.CenterEnd) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.selected),
                        tint = selectedColor,
                    )
                }
            }
        }

        // Cover
        Box(
            modifier =
                Modifier.padding(start = Size.small, top = Size.extraTiny)
                    .background(color = DividerDefaults.color, shape = MaterialTheme.shapes.small)
                    .fillMaxWidth(0.5f)
                    .aspectRatio(MangaCover.Book.ratio)
        ) {
            Row(
                modifier =
                    Modifier.padding(Size.small)
                        .size(width = Size.large, height = Size.medium)
                        .clip(RoundedCornerShape(Size.small))
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxHeight()
                            .width(Size.smedium)
                            .background(colorScheme.tertiary)
                )
                Box(
                    modifier =
                        Modifier.fillMaxHeight()
                            .width(Size.smedium)
                            .background(colorScheme.secondary)
                )
            }
        }

        // Bottom bar
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(tonalElevation = Size.small) {
                Row(
                    modifier =
                        Modifier.height(Size.extraLarge)
                            .fillMaxWidth()
                            .background(colorScheme.surfaceVariant)
                            .padding(horizontal = Size.small),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier.size(Size.medium)
                                .background(
                                    color =
                                        colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaHighContrast
                                        ),
                                    shape = CircleShape,
                                )
                    )
                    Box(
                        modifier =
                            Modifier.size(Size.medium)
                                .background(color = colorScheme.primary, shape = CircleShape)
                    )
                    Box(
                        modifier =
                            Modifier.size(Size.medium)
                                .background(
                                    color =
                                        colorScheme.onSurface.copy(
                                            alpha = NekoColors.mediumAlphaHighContrast
                                        ),
                                    shape = CircleShape,
                                )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewThemeItem() {
    Surface() {
        ThemeItem(theme = Themes.SPRING_AND_DUSK, isDarkTheme = false, selected = false) {}
    }
}
