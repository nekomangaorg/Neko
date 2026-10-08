package org.nekomanga.presentation.screens.reader

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.R

class ReaderChaptersSheetUiStateTest {

    @Test
    fun `given default ReaderChaptersSheetUiState, verify default properties`() {
        val state = ReaderChaptersSheetUiState()

        assertTrue(state.chapters.isEmpty())
        assertTrue(state.quickActions.isEmpty())
        assertEquals(-1, state.currentChapterIndex)
        assertFalse(state.isLoading)
        assertNull(state.loadingChapterId)
    }

    @Test
    fun `given ReaderChaptersSheetUiState, when copied, updates properties while preserving others`() {
        val initial =
            ReaderChaptersSheetUiState(
                chapters =
                    listOf(
                        ReaderChapterRowUiModel(
                            id = 1L,
                            formattedTitle = "Chapter 1",
                            formattedSubtitle = null,
                            isCurrent = true,
                            isRead = false,
                            isBookmarked = false,
                            textColor = Color.White,
                            bookmarkColor = Color.Yellow,
                        )
                    ),
                quickActions =
                    listOf(
                        ReaderQuickActionUiModel(
                            id = ReaderQuickActionId.Comments,
                            iconRes = R.drawable.ic_view_comments_24p,
                            tooltipRes = R.string.comments,
                        )
                    ),
                currentChapterIndex = 0,
                isLoading = false,
            )

        val updated = initial.copy(currentChapterIndex = 1, isLoading = true)

        assertEquals(1, updated.chapters.size)
        assertEquals(1, updated.quickActions.size)
        assertEquals(1, updated.currentChapterIndex)
        assertTrue(updated.isLoading)
        assertNotEquals(initial, updated)
    }

    @Test
    fun `given identical ReaderChaptersSheetUiState, equals and hashCode contract is satisfied`() {
        val state1 =
            ReaderChaptersSheetUiState(
                currentChapterIndex = 5,
                isLoading = false,
            )
        val state2 =
            ReaderChaptersSheetUiState(
                currentChapterIndex = 5,
                isLoading = false,
            )

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun `given ReaderChapterRowUiModel, properties are accurately exposed`() {
        val model =
            ReaderChapterRowUiModel(
                id = 42L,
                formattedTitle = "Chapter 42 - The Answer",
                formattedSubtitle = "Yesterday • Scan Group",
                isCurrent = true,
                isRead = false,
                isBookmarked = true,
                textColor = Color.Green,
                bookmarkColor = Color.Red,
                language = "Spanish",
            )

        assertEquals(42L, model.id)
        assertEquals("Chapter 42 - The Answer", model.formattedTitle)
        assertEquals("Yesterday • Scan Group", model.formattedSubtitle)
        assertTrue(model.isCurrent)
        assertFalse(model.isRead)
        assertTrue(model.isBookmarked)
        assertEquals(Color.Green, model.textColor)
        assertEquals(Color.Red, model.bookmarkColor)
        assertEquals("Spanish", model.language)
    }

    @Test
    fun `given ReaderChapterRowUiModel, copy and equality contract is satisfied`() {
        val model1 =
            ReaderChapterRowUiModel(
                id = 10L,
                formattedTitle = "Ch. 10",
                formattedSubtitle = null,
                isCurrent = false,
                isRead = true,
                isBookmarked = false,
                textColor = Color.Gray,
                bookmarkColor = Color.DarkGray,
            )
        val model2 = model1.copy(isBookmarked = true)

        assertNotEquals(model1, model2)
        assertTrue(model2.isBookmarked)
        assertEquals(model1.formattedTitle, model2.formattedTitle)
    }

    @Test
    fun `given ReaderChapterRowUiModel with defaults, textColor and bookmarkColor default to null`() {
        val model =
            ReaderChapterRowUiModel(
                id = 1L,
                formattedTitle = "Chapter 1",
                formattedSubtitle = null,
                isCurrent = false,
                isRead = false,
                isBookmarked = false,
            )

        assertNull(model.textColor)
        assertNull(model.bookmarkColor)
    }

    @Test
    fun `given ReaderQuickActionUiModel with resource constructor, icon is Resource and iconRes matches`() {
        val model =
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Comments,
                iconRes = R.drawable.ic_view_comments_24p,
                tooltipRes = R.string.comments,
            )

        assertEquals(ReaderQuickActionId.Comments, model.id)
        assertTrue(model.icon is ReaderButtonIcon.Resource)
        assertEquals(R.drawable.ic_view_comments_24p, model.iconRes)
        assertNull(model.iconVector)
        assertEquals(R.string.comments, model.tooltipRes)
        assertTrue(model.isEnabled)
        assertFalse(model.isToggled)
        assertEquals(ButtonToggleStyle.None, model.toggleStyle)
    }

    @Test
    fun `given ReaderQuickActionUiModel with vector constructor, icon is Vector and iconVector matches`() {
        val model =
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.CropBorders,
                iconVector = Icons.Default.Crop,
                tooltipRes = R.string.crop_borders,
                toggleStyle = ButtonToggleStyle.PrimaryWhenToggled,
                isToggled = true,
            )

        assertEquals(ReaderQuickActionId.CropBorders, model.id)
        assertTrue(model.icon is ReaderButtonIcon.Vector)
        assertEquals(Icons.Default.Crop, model.iconVector)
        assertEquals(0, model.iconRes)
        assertEquals(R.string.crop_borders, model.tooltipRes)
        assertTrue(model.isEnabled)
        assertTrue(model.isToggled)
        assertEquals(ButtonToggleStyle.PrimaryWhenToggled, model.toggleStyle)
    }

    @Test
    fun `given ReaderQuickActionUiModel, copy and equality contract is satisfied`() {
        val model1 =
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Rotation,
                iconRes = R.drawable.ic_screen_rotation_24dp,
                tooltipRes = R.string.rotation,
            )
        val model2 =
            ReaderQuickActionUiModel(
                id = ReaderQuickActionId.Rotation,
                iconRes = R.drawable.ic_screen_rotation_24dp,
                tooltipRes = R.string.rotation,
            )

        assertEquals(model1, model2)
        assertEquals(model1.hashCode(), model2.hashCode())

        val modified = model1.copy(isEnabled = false)
        assertNotEquals(model1, modified)
        assertFalse(modified.isEnabled)
    }

    @Test
    fun `given ReaderChaptersAction instances, verify types and payloads`() {
        val select = ReaderChaptersAction.SelectChapter(100L)
        val bookmark = ReaderChaptersAction.ToggleBookmark(200L)
        val quickAction = ReaderChaptersAction.QuickActionClick(ReaderQuickActionId.Grayscale)
        val dismiss = ReaderChaptersAction.Dismiss

        assertEquals(100L, select.chapterId)
        assertEquals(200L, bookmark.chapterId)
        assertEquals(ReaderQuickActionId.Grayscale, quickAction.actionId)
        assertEquals(ReaderChaptersAction.Dismiss, dismiss)
    }

    @Test
    fun `given default buildReaderChaptersQuickActions parameters, returns basic actions in expected order`() {
        val actions = buildReaderChaptersQuickActions()

        val expectedIds =
            listOf(
                ReaderQuickActionId.Chapters,
                ReaderQuickActionId.Comments,
                ReaderQuickActionId.WebView,
                ReaderQuickActionId.DisplayOptions,
            )

        assertEquals(expectedIds, actions.map { it.id })
    }

    @Test
    fun `given all flags enabled, buildReaderChaptersQuickActions returns all 10 quick actions`() {
        val actions =
            buildReaderChaptersQuickActions(
                isChaptersVisible = true,
                isCommentsVisible = true,
                isWebViewVisible = true,
                isReadingModeVisible = true,
                readingModeIconRes = R.drawable.ic_reader_default_24dp,
                isRotationVisible = true,
                rotationIconRes = R.drawable.ic_screen_rotation_24dp,
                isCropBordersVisible = true,
                cropBorders = true,
                isGrayscaleVisible = true,
                grayscale = true,
                isDoublePageVisible = true,
                doublePageIconRes = R.drawable.ic_book_open_variant_24dp,
                isShiftPageVisible = true,
                shiftPageIconRes = R.drawable.ic_page_next_outline_24dp,
                isDisplayOptionsVisible = true,
            )

        assertEquals(10, actions.size)
        val actionIds = actions.map { it.id }
        assertEquals(
            listOf(
                ReaderQuickActionId.Chapters,
                ReaderQuickActionId.Comments,
                ReaderQuickActionId.WebView,
                ReaderQuickActionId.ReadingMode,
                ReaderQuickActionId.Rotation,
                ReaderQuickActionId.CropBorders,
                ReaderQuickActionId.Grayscale,
                ReaderQuickActionId.DoublePage,
                ReaderQuickActionId.ShiftPage,
                ReaderQuickActionId.DisplayOptions,
            ),
            actionIds,
        )

        val cropAction = actions.first { it.id == ReaderQuickActionId.CropBorders }
        assertEquals(Icons.Default.CropFree, cropAction.iconVector)
        assertTrue(cropAction.isToggled)
        assertEquals(ButtonToggleStyle.PrimaryWhenToggled, cropAction.toggleStyle)

        val grayscaleAction = actions.first { it.id == ReaderQuickActionId.Grayscale }
        assertTrue(grayscaleAction.isToggled)
        assertEquals(ButtonToggleStyle.PrimaryWhenToggled, grayscaleAction.toggleStyle)
    }

    @Test
    fun `given all flags disabled, buildReaderChaptersQuickActions returns only DisplayOptions when enabled`() {
        val actions =
            buildReaderChaptersQuickActions(
                isChaptersVisible = false,
                isCommentsVisible = false,
                isWebViewVisible = false,
                isReadingModeVisible = false,
                isRotationVisible = false,
                isCropBordersVisible = false,
                isGrayscaleVisible = false,
                isDoublePageVisible = false,
                isShiftPageVisible = false,
                isDisplayOptionsVisible = true,
            )

        assertEquals(1, actions.size)
        assertEquals(ReaderQuickActionId.DisplayOptions, actions.first().id)
    }

    @Test
    fun `given cropBorders false, CropBorders quick action uses Crop icon and isToggled false`() {
        val actions =
            buildReaderChaptersQuickActions(
                isCropBordersVisible = true,
                cropBorders = false,
            )

        val cropAction = actions.first { it.id == ReaderQuickActionId.CropBorders }
        assertEquals(Icons.Default.Crop, cropAction.iconVector)
        assertFalse(cropAction.isToggled)
    }

    @Test
    fun `given hideChapterTitles false, toReaderChapterRowUiModel keeps original chapter name`() {
        val chapter =
            Chapter.create().apply {
                id = 1L
                name = "Chapter 1: The Awakening"
                chapter_number = 1.0f
            }
        val manga = Manga.create(0L).apply { id = 100L }
        val item = ReaderChapterItem(chapter, manga, isCurrent = false)

        val context = mockk<Context>()
        val rowModel =
            item.toReaderChapterRowUiModel(
                context = context,
                hideChapterTitles = false,
                textColor = Color.White,
                bookmarkColor = Color.Yellow,
            )

        assertEquals(1L, rowModel.id)
        assertEquals("Chapter 1: The Awakening", rowModel.formattedTitle)
        assertFalse(rowModel.isCurrent)
        assertFalse(rowModel.isRead)
        assertFalse(rowModel.isBookmarked)
    }

    @Test
    fun `given hideChapterTitles true, toReaderChapterRowUiModel formats chapter number with decimal format`() {
        val chapter =
            Chapter.create().apply {
                id = 5L
                name = "Detailed Episode Title"
                chapter_number = 5.5f
            }
        val manga = Manga.create(0L).apply { id = 100L }
        val item = ReaderChapterItem(chapter, manga, isCurrent = true)

        val context = mockk<Context>()
        every { context.getString(R.string.chapter_, "5.5") } returns "Chapter 5.5"

        val rowModel =
            item.toReaderChapterRowUiModel(
                context = context,
                hideChapterTitles = true,
                textColor = Color.Green,
                bookmarkColor = Color.Cyan,
            )

        assertEquals(5L, rowModel.id)
        assertEquals("Chapter 5.5", rowModel.formattedTitle)
        assertTrue(rowModel.isCurrent)
    }

    @Test
    fun `given chapter with scanlator and without date, toReaderChapterRowUiModel formats scanlator subtitle`() {
        val chapter =
            Chapter.create().apply {
                id = 2L
                name = "Chapter 2"
                chapter_number = 2.0f
                scanlator = "Flame Scans"
                date_upload = 0L
            }
        val manga = Manga.create(0L).apply { id = 100L }
        val item = ReaderChapterItem(chapter, manga, isCurrent = false)

        val context = mockk<Context>()
        val rowModel =
            item.toReaderChapterRowUiModel(
                context = context,
                hideChapterTitles = false,
                textColor = Color.White,
                bookmarkColor = Color.Yellow,
            )

        assertEquals("Flame Scans", rowModel.formattedSubtitle)
    }

    @Test
    fun `given chapter without scanlator and date_upload 0, formattedSubtitle is null`() {
        val chapter =
            Chapter.create().apply {
                id = 3L
                name = "Chapter 3"
                chapter_number = 3.0f
                scanlator = ""
                date_upload = 0L
            }
        val manga = Manga.create(0L).apply { id = 100L }
        val item = ReaderChapterItem(chapter, manga, isCurrent = false)

        val context = mockk<Context>()
        val rowModel =
            item.toReaderChapterRowUiModel(
                context = context,
                hideChapterTitles = false,
                textColor = Color.White,
                bookmarkColor = Color.Yellow,
            )

        assertNull(rowModel.formattedSubtitle)
    }

    @Test
    fun `given chapter with English language, language is filtered to null`() {
        val englishVariants = listOf("english", "English", "ENGLISH", "en", "EN", "En")
        val context = mockk<Context>()

        englishVariants.forEach { lang ->
            val chapter =
                Chapter.create().apply {
                    id = 10L
                    name = "Chapter"
                    chapter_number = 1.0f
                    language = lang
                }
            val manga = Manga.create(0L).apply { id = 100L }
            val item = ReaderChapterItem(chapter, manga, isCurrent = false)

            val rowModel =
                item.toReaderChapterRowUiModel(
                    context = context,
                    hideChapterTitles = false,
                    textColor = Color.White,
                    bookmarkColor = Color.Yellow,
                )

            assertNull("Expected language '$lang' to be filtered to null", rowModel.language)
        }
    }

    @Test
    fun `given chapter with non-English language, language is preserved`() {
        val nonEnglishLanguages = listOf("Spanish", "ja", "pt-br", "Russian", "fr")
        val context = mockk<Context>()

        nonEnglishLanguages.forEach { lang ->
            val chapter =
                Chapter.create().apply {
                    id = 11L
                    name = "Chapter"
                    chapter_number = 1.0f
                    language = lang
                }
            val manga = Manga.create(0L).apply { id = 100L }
            val item = ReaderChapterItem(chapter, manga, isCurrent = false)

            val rowModel =
                item.toReaderChapterRowUiModel(
                    context = context,
                    hideChapterTitles = false,
                    textColor = Color.White,
                    bookmarkColor = Color.Yellow,
                )

            assertEquals(lang, rowModel.language)
        }
    }

    @Test
    fun `given chapter read and bookmarked flags, toReaderChapterRowUiModel accurately reflects states`() {
        val chapter =
            Chapter.create().apply {
                id = 99L
                name = "Chapter 99"
                chapter_number = 99.0f
                read = true
                bookmark = true
            }
        val manga = Manga.create(0L).apply { id = 100L }
        val item = ReaderChapterItem(chapter, manga, isCurrent = false)

        val context = mockk<Context>()
        val rowModel =
            item.toReaderChapterRowUiModel(
                context = context,
                hideChapterTitles = false,
                textColor = Color.LightGray,
                bookmarkColor = Color.Magenta,
            )

        assertTrue(rowModel.isRead)
        assertTrue(rowModel.isBookmarked)
        assertEquals(Color.LightGray, rowModel.textColor)
        assertEquals(Color.Magenta, rowModel.bookmarkColor)
    }

    @Test
    fun `given toReaderChapterRowUiModel without colors, textColor and bookmarkColor default to null`() {
        val chapter =
            Chapter.create().apply {
                id = 1L
                name = "Chapter 1"
                chapter_number = 1.0f
            }
        val manga = Manga.create(0L).apply { id = 100L }
        val item = ReaderChapterItem(chapter, manga, isCurrent = false)
        val context = mockk<Context>()

        val rowModel = item.toReaderChapterRowUiModel(context = context)

        assertNull(rowModel.textColor)
        assertNull(rowModel.bookmarkColor)
    }
}
