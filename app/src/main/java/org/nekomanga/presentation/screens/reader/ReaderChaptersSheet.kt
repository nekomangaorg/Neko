package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.nekomanga.R
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.ThemeConfig
import org.nekomanga.presentation.theme.ThemeConfigProvider
import org.nekomanga.presentation.theme.ThemedPreviews

@Composable
fun ReaderChaptersSheet(
    uiState: ReaderChaptersSheetUiState,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    onAction: (ReaderChaptersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val maxLazyHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    var hasScrolledToCurrent by remember { mutableStateOf(false) }

    // Scroll to the current reading chapter only on initial load
    LaunchedEffect(uiState.chapters.isNotEmpty(), uiState.currentChapterIndex) {
        if (!hasScrolledToCurrent && uiState.chapters.isNotEmpty()) {
            val currentIndex =
                if (uiState.currentChapterIndex >= 0) {
                    uiState.currentChapterIndex
                } else {
                    uiState.chapters.indexOfFirst { it.isCurrent }
                }
            if (currentIndex >= 0) {
                val scrollIndex = maxOf(0, currentIndex - 2)
                listState.scrollToItem(scrollIndex)
                hasScrolledToCurrent = true
            }
        }
    }

    val onSelectChapter: (Long) -> Unit =
        remember(onAction) { { id -> onAction(ReaderChaptersAction.SelectChapter(id)) } }
    val onToggleBookmark: (Long) -> Unit =
        remember(onAction) { { id -> onAction(ReaderChaptersAction.ToggleBookmark(id)) } }

    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = 0.9f,
        bottomPaddingAroundContent = Size.none,
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(vertical = Size.small)) {
            // Drag handle pill is drawn by BaseSheet, so we build the header shortcuts row
            if (uiState.quickActions.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier.widthIn(min = maxWidth)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = Size.medium, vertical = Size.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        uiState.quickActions.forEach { action ->
                            val tint =
                                when (action.toggleStyle) {
                                    ButtonToggleStyle.PrimaryWhenToggled ->
                                        if (action.isToggled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    ButtonToggleStyle.None -> MaterialTheme.colorScheme.primary
                                }
                            val onClick = {
                                onAction(ReaderChaptersAction.QuickActionClick(action.id))
                            }
                            if (action.iconVector != null) {
                                ToolTipButton(
                                    toolTipLabel = stringResource(action.tooltipRes),
                                    icon = action.iconVector,
                                    isEnabled = action.isEnabled,
                                    enabledTint = tint,
                                    onClick = onClick,
                                )
                            } else if (action.iconRes != 0) {
                                ToolTipButton(
                                    toolTipLabel = stringResource(action.tooltipRes),
                                    painter = painterResource(action.iconRes),
                                    isEnabled = action.isEnabled,
                                    enabledTint = tint,
                                    onClick = onClick,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
            }

            Box(modifier = Modifier.fillMaxWidth().heightIn(max = maxLazyHeight)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(
                        items = uiState.chapters,
                        key = { _, item -> item.id },
                    ) { _, item ->
                        val isLoading =
                            uiState.loadingChapterId == item.id ||
                                (uiState.isLoading && item.isCurrent)
                        ChapterListItem(
                            model = item,
                            isLoading = isLoading,
                            themeColorState = themeColorState,
                            onSelectChapter = onSelectChapter,
                            onToggleBookmark = onToggleBookmark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    model: ReaderChapterRowUiModel,
    isLoading: Boolean,
    themeColorState: ThemeColorState,
    onSelectChapter: (Long) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontStyle = if (model.isCurrent) FontStyle.Italic else FontStyle.Normal
    val fontWeight = if (model.isCurrent) FontWeight.Bold else FontWeight.Normal
    val textColor =
        model.textColor
            ?: when {
                model.isCurrent -> themeColorState.primaryColor
                model.isRead -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
    val bookmarkColor =
        model.bookmarkColor
            ?: when {
                model.isBookmarked -> themeColorState.primaryColor
                else -> MaterialTheme.colorScheme.outline
            }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) { onSelectChapter(model.id) }
                .padding(horizontal = Size.medium, vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.formattedTitle,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        color = textColor,
                    ),
            )
            val hasSubtitle = !model.formattedSubtitle.isNullOrBlank()
            val hasLanguage = !model.language.isNullOrBlank()
            if (hasSubtitle || hasLanguage) {
                Spacer(modifier = Modifier.height(Size.extraTiny))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasLanguage) {
                        Text(
                            text = model.language,
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    color = textColor,
                                ),
                            modifier = Modifier.padding(end = Size.small),
                        )
                    }
                    if (hasSubtitle) {
                        Text(
                            text = model.formattedSubtitle,
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    color = textColor,
                                ),
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Size.large),
                strokeWidth = Size.extraTiny,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            IconButton(onClick = { onToggleBookmark(model.id) }) {
                Icon(
                    painter =
                        painterResource(
                            if (model.isBookmarked) R.drawable.ic_bookmark_24dp
                            else R.drawable.ic_bookmark_border_24dp
                        ),
                    contentDescription =
                        stringResource(
                            if (model.isBookmarked) R.string.bookmarked else R.string.not_bookmarked
                        ),
                    tint = bookmarkColor,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReaderChaptersSheetPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        val quickActions = buildReaderChaptersQuickActions()
        val chapters =
            listOf(
                ReaderChapterRowUiModel(
                    id = 1L,
                    formattedTitle = "Chapter 1 - The Beginning",
                    formattedSubtitle = "2 days ago • MangaDex",
                    isCurrent = false,
                    isRead = true,
                    isBookmarked = false,
                ),
                ReaderChapterRowUiModel(
                    id = 2L,
                    formattedTitle = "Chapter 2 - Next Step",
                    formattedSubtitle = "Yesterday • Scan Group",
                    isCurrent = true,
                    isRead = false,
                    isBookmarked = true,
                ),
                ReaderChapterRowUiModel(
                    id = 3L,
                    formattedTitle = "Chapter 3 - The Encounter",
                    formattedSubtitle = "1 hour ago • Official",
                    isCurrent = false,
                    isRead = false,
                    isBookmarked = false,
                ),
            )
        ReaderChaptersSheet(
            uiState =
                ReaderChaptersSheetUiState(
                    chapters = chapters,
                    quickActions = quickActions,
                    currentChapterIndex = 1,
                ),
            onAction = {},
        )
    }
}
