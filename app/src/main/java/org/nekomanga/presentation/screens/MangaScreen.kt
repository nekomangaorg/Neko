package org.nekomanga.presentation.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import kotlinx.coroutines.launch
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.PrimaryColorRippleTheme
import org.nekomanga.presentation.components.sheets.EditCategorySheet
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.Shapes

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaScreen(
    manga: Manga,
    categories: List<Category>,
    themeBasedOffCover: Boolean = true,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (Context, String) -> Unit,
    creatorLongClick: (Context, String) -> Unit,
    trackServiceCount: Int,
    toggleFavorite: () -> Boolean = { true },
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
    onBackPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    var currentBottomSheet: BottomSheetScreen? by remember {
        mutableStateOf(null)
    }

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

    val rippleTheme = when (buttonColor != secondaryColor) {
        true -> DynamicRippleTheme(buttonColor)
        false -> PrimaryColorRippleTheme
    }

    val themeColors = ThemeColors(buttonColor, rippleTheme)

    //set the current sheet to null when bottom sheet is closed
    if (sheetState.isVisible.not()) {
        currentBottomSheet = null
    }

    val openSheet: (BottomSheetScreen) -> Unit = {
        scope.launch {
            currentBottomSheet = it
            sheetState.show()
        }
    }
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(Shapes.sheetRadius),
        sheetContent = {
            Box(modifier = Modifier.defaultMinSize(minHeight = 1.dp)) {
                currentBottomSheet?.let { currentSheet ->
                    SheetLayout(currentSheet, themeColors) { scope.launch { sheetState.hide() } }
                }
            }
        },
    ) {

        NekoScaffold(
            title = "",
            onNavigationIconClicked = onBackPressed,
            actions = {

            },
        ) {

            val contentPadding =
                PaddingValues(
                    bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        .asPaddingValues().calculateBottomPadding(),
                )

            val context = LocalContext.current

            var inLibrary by remember { mutableStateOf(manga.favorite) }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                item {
                    MangaDetailsHeader(
                        manga = manga,
                        inLibrary = inLibrary,
                        titleLongClick = { title -> titleLongClick(context, title) },
                        creatorLongClick = { creator -> creatorLongClick(context, creator) },
                        themeColor = themeColors,
                        generatePalette = generatePalette,
                        trackServiceCount = trackServiceCount,
                        toggleFavorite = { inLibrary = toggleFavorite() },
                        categories = categories,
                        moveCategories = { openSheet(BottomSheetScreen.CategoriesSheet) },
                        trackingClick = trackingClick,
                        artworkClick = artworkClick,
                        similarClick = similarClick,
                        mergeClick = mergeClick,
                        linksClick = linksClick,
                        shareClick = shareClick,
                        genreClick = genreClick,
                        genreLongClick = genreLongClick,
                        quickReadClick = quickReadClick,
                        quickReadText = quickReadText,
                        numberOfChapters = numberOfChapters,
                        chapterHeaderClick = chapterHeaderClick,
                        chapterFilterText = chapterFilterText,
                    )

                }
            }
        }

    }
}

@Composable
fun SheetLayout(currentScreen: BottomSheetScreen, themeColor: ThemeColors, closeSheet: () -> Unit) {
    when (currentScreen) {
        BottomSheetScreen.CategoriesSheet -> EditCategorySheet(themeColor, closeSheet)
    }
}

sealed class BottomSheetScreen {
    object CategoriesSheet : BottomSheetScreen()
    //object Screen2 : BottomSheetScreen()
    // class Screen3(val argument: String) : BottomSheetScreen()
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

data class ThemeColors(val buttonColor: Color, val rippleTheme: RippleTheme)

