package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import jp.wasabeef.gap.Gap
import me.saket.cascade.CascadeDropdownMenu
import onColor
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun MangaDetailsHeader(
    manga: Manga,
    inLibrary: Boolean = true,
    themeColor: ThemeColors,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (String) -> Unit = {},
    creatorLongClick: (String) -> Unit = {},
    loggedIntoTrackers: Boolean,
    trackServiceCount: Int,
    toggleFavorite: () -> Unit = {},
    categories: List<Category> = emptyList(),
    moveCategories: () -> Unit = {},
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

    var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

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
                themeColor = themeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(250.dp, 400.dp),
                generatePalette = generatePalette,
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
                    inLibrary = inLibrary,
                    loggedIntoTrackers = loggedIntoTrackers,
                    trackServiceCount = trackServiceCount,
                    themeColor = themeColor,
                    favoriteClick = {
                        if (inLibrary.not()) {
                            toggleFavorite()
                        } else {
                            favoriteExpanded = true
                        }
                    },
                    trackingClick = trackingClick,
                    artworkClick = artworkClick,
                    similarClick = similarClick,
                    mergeClick = mergeClick,
                    linksClick = linksClick,
                    shareClick = shareClick,
                )
                FavoriteDropDown(
                    favoriteExpanded = favoriteExpanded,
                    categories = categories,
                    moveCategories = moveCategories,
                    toggleFavorite = toggleFavorite,
                    onDismiss = { favoriteExpanded = false },
                )
            }
        }
        Gap(16.dp)
        DescriptionBlock(
            manga = manga,
            themeColor = themeColor,
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
                colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColor.buttonColor),
            ) {
                Text(text = quickReadText, style = MaterialTheme.typography.titleMedium, color = themeColor.buttonColor.onColor())
            }
        }

        Gap(8.dp)
        ChapterHeader(themeColor = themeColor, numberOfChapters = numberOfChapters, filterText = chapterFilterText, onClick = chapterHeaderClick)
    }
}

@Composable
private fun FavoriteDropDown(favoriteExpanded: Boolean, categories: List<Category>, moveCategories: () -> Unit, toggleFavorite: () -> Unit, onDismiss: () -> Unit) {
    CascadeDropdownMenu(
        expanded = favoriteExpanded,
        offset = DpOffset(8.dp, 0.dp),
        onDismissRequest = onDismiss,
    ) {
        val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-.5).sp)
        androidx.compose.material3.DropdownMenuItem(
            text = {
                androidx.compose.material.Text(
                    text = stringResource(R.string.remove_from_library),
                    style = style,
                )
            },
            onClick = {
                toggleFavorite()
                onDismiss()
            },
        )
        if (categories.isNotEmpty()) {
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    androidx.compose.material.Text(
                        text = stringResource(R.string.edit_categories),
                        style = style,
                    )
                },
                onClick = {
                    moveCategories()
                    onDismiss()
                },
            )
        }
    }
}

