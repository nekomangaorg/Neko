In `MangaScreen.kt`, we'll modify the `chapterList` signature:
```kotlin
private fun LazyListScope.chapterList(
    chapterGroups: Map<String, List<ChapterItem>>,
    screenState: MangaConstants.MangaDetailScreenState,
    themeColorState: ThemeColorState,
    chapterActions: ChapterActions,
    onBookmark: (ChapterItem) -> Unit,
    onRead: (ChapterItem) -> Unit,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    item(key = "chapter_header") {
        val totalChapters = chapterGroups.values.sumOf { it.size }
        ChapterHeader(
            themeColor = themeColorState,
            numberOfChapters = totalChapters,
            filterText = screenState.chapters.chapterFilterText,
            onClick = { onOpenSheet(DetailsBottomSheetScreen.FilterChapterSheet) },
        )
    }

    chapterGroups.forEach { (volume, chapters) ->
        item(key = "volume_$volume") {
            // Volume Header Card
            ExpressiveListCard(
                modifier = Modifier.padding(horizontal = Size.small, vertical = Size.tiny),
                listCardType = ListCardType.Single,
                themeColorState = themeColorState,
            ) {
                // UI for volume
                Text(
                    text = if (volume == "No Volume") "No Volume" else "Vol. $volume",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(Size.medium)
                )
            }
        }

        itemsIndexed(items = chapters, key = { _, chapter -> chapter.chapter.id }) { index, chapterItem ->
            MangaChapterListItem(
                index = index,
                chapterItem = chapterItem,
                count = chapters.size,
                themeColorState = themeColorState,
                shouldHideChapterTitles =
                    screenState.chapters.chapterFilter.hideChapterTitles == ToggleableState.On,
                chapterActions = chapterActions,
                onBookmark = onBookmark,
                onRead = onRead,
            )
        }
    }
}
```

Wait, `ExpressiveListCard` expects a composable. We need to import `Text`, `Modifier`, `Size` etc. if not already imported.

In `VerticalLayout` and `SideBySideLayout`, we call `chapterList` like this:
```kotlin
                if (isInitialized) {
                    val chapters = if (screenState.general.isSearching) screenState.general.searchChapters else screenState.chapters.activeChapters
                    val chapterGroups = chapters.groupBy { screenState.chapters.chapterVolumes[it.chapter.id] ?: "No Volume" }
                    chapterList(
                        chapterGroups = chapterGroups,
                        ...
                    )
                }
```
