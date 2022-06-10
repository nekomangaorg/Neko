package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.data.database.models.Manga
import jp.wasabeef.gap.Gap
import onColor

@Composable
fun MangaDetailsHeader(
    manga: Manga,
    themeBasedOffCover: Boolean = true,
    titleLongClick: (String) -> Unit = {},
    creatorLongClick: (String) -> Unit = {},
    trackServiceCount: Int,
    favoriteClick: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
    genreClick: (String) -> Unit = {},
    genreLongClick: (String) -> Unit = {},
    quickReadText: String = "",
    quickReadClick: () -> Unit = {},
    numberOfChapters: Int,
    chapterHeaderClick: () -> Unit = {},
    chapterFilterText: String,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val buttonColor = remember {
        when (themeBasedOffCover && manga.vibrantCoverColor != null) {
            true -> {
                val color = getButtonThemeColor(Color(manga.vibrantCoverColor!!), isDarkTheme)
                color
            }
            false -> secondaryColor
        }
    }
    val isTablet = LocalConfiguration.current.screenWidthDp.dp >= 600.dp

    val isExpanded = rememberSaveable {
        when (isTablet) {
            false -> mutableStateOf(manga.favorite.not())
            true -> mutableStateOf(true)
        }
    }

    Column {
        Box {
            BackDrop(
                manga = manga,
                themeBasedOffCover = themeBasedOffCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(250.dp, 400.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                        ),
                    ),
            )
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                InformationBlock(
                    manga = manga,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 70.dp),
                    isExpanded = isExpanded.value,
                    titleLongClick = titleLongClick,
                    creatorLongClicked = creatorLongClick,
                )
                Gap(height = 24.dp)
                ButtonBlock(
                    manga = manga,
                    trackServiceCount = trackServiceCount,
                    buttonColor = buttonColor,
                    favoriteClick = favoriteClick,
                    trackingClick = trackingClick,
                    artworkClick = artworkClick,
                    similarClick = similarClick,
                    mergeClick = mergeClick,
                    linksClick = linksClick,
                    shareClick = shareClick,
                )
            }
        }
        Gap(16.dp)
        DescriptionBlock(
            manga = manga,
            buttonColor = buttonColor,
            isExpanded = isExpanded.value,
            isTablet = isTablet,
            expandCollapseClick = {
                //dont expand/collapse when tablet
                if (isTablet.not()) {
                    isExpanded.value = isExpanded.value.not()
                }
            },
            genreClick = genreClick,
            genreLongClick = genreLongClick,
        )
        Gap(16.dp)
        if (quickReadText.isNotEmpty()) {
            ElevatedButton(
                onClick = quickReadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = buttonColor),
            ) {
                Text(text = quickReadText, style = MaterialTheme.typography.titleMedium, color = buttonColor.onColor())
            }
        }

        Gap(8.dp)
        ChapterHeader(buttonColor = buttonColor, numberOfChapters = numberOfChapters, filterText = chapterFilterText, onClick = chapterHeaderClick)
    }
}

private fun getButtonThemeColor(buttonColor: Color, isNightMode: Boolean): Color {

    val color1 = buttonColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(color1).toFloat()

    val color2 = when (isNightMode) {
        true -> Color.White.toArgb()
        false -> Color.Black.toArgb()
    }

    val ratio = when (isNightMode) {
        true -> (-(luminance - 1)) * .33f
        false -> luminance * .5f
    }

    return when ((isNightMode && luminance <= 0.6) || (isNightMode.not() && luminance > 0.4)) {
        true -> Color(ColorUtils.blendARGB(color1, color2, ratio))
        false -> buttonColor
    }
}
