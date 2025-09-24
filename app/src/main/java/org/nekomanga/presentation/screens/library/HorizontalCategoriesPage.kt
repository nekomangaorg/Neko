package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.MangaGridItem
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Size

@Composable
fun HorizontalCategoriesPage(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val columns = numberOfColumns(rawValue = libraryScreenState.rawColumnCount)
    val selectedIds = remember(libraryScreenState.selectedItems) {
        libraryScreenState.selectedItems.map { it.displayManga.mangaId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
        ) {
            libraryScreenState.items.forEachIndexed { index, item ->
                Tab(
                    text = { Text(item.categoryItem.name) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                )
            }
        }

        HorizontalPager(
            count = libraryScreenState.items.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = libraryScreenState.items[page]
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
                horizontalArrangement = Arrangement.spacedBy(Size.small),
                contentPadding = contentPadding,
            ) {
                items(items = item.libraryItems) { libraryItem ->
                    MangaGridItem(
                        displayManga = libraryItem.displayManga,
                        showUnreadBadge = libraryScreenState.showUnreadBadges,
                        unreadCount = libraryItem.unreadCount,
                        showDownloadBadge = libraryScreenState.showDownloadBadges,
                        downloadCount = libraryItem.downloadCount,
                        shouldOutlineCover = libraryScreenState.outlineCovers,
                        isComfortable = libraryScreenState.libraryDisplayMode is LibraryDisplayMode.ComfortableGrid,
                        isSelected = selectedIds.contains(libraryItem.displayManga.mangaId),
                        showStartReadingButton = libraryScreenState.showStartReadingButton && libraryItem.unreadCount > 0,
                        onStartReadingClick = {
                            libraryScreenActions.mangaStartReadingClick(libraryItem.displayManga.mangaId)
                        },
                        onClick = {
                            if (libraryScreenState.selectedItems.isNotEmpty()) {
                                libraryScreenActions.mangaLongClick(libraryItem)
                            } else {
                                libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId)
                            }
                        },
                        onLongClick = { libraryScreenActions.mangaLongClick(libraryItem) },
                    )
                }
            }
        }
    }
}
