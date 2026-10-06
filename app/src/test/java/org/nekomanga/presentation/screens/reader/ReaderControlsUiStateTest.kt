package org.nekomanga.presentation.screens.reader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import eu.kanade.tachiyomi.ui.reader.settings.ReaderSliderPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.R

class ReaderControlsUiStateTest {

    @Test
    fun `given isVertical false, SliderOrientation is always Horizontal regardless of preference`() {
        assertEquals(
            SliderOrientation.Horizontal,
            SliderOrientation.from(isVertical = false, sliderPosition = ReaderSliderPosition.LEFT),
        )
        assertEquals(
            SliderOrientation.Horizontal,
            SliderOrientation.from(isVertical = false, sliderPosition = ReaderSliderPosition.RIGHT),
        )
        assertEquals(
            SliderOrientation.Horizontal,
            SliderOrientation.from(
                isVertical = false,
                sliderPosition = ReaderSliderPosition.HORIZONTAL,
            ),
        )
    }

    @Test
    fun `given isVertical true, SliderOrientation maps correctly to vertical or horizontal`() {
        assertEquals(
            SliderOrientation.VerticalLeft,
            SliderOrientation.from(isVertical = true, sliderPosition = ReaderSliderPosition.LEFT),
        )
        assertEquals(
            SliderOrientation.VerticalRight,
            SliderOrientation.from(isVertical = true, sliderPosition = ReaderSliderPosition.RIGHT),
        )
        assertEquals(
            SliderOrientation.Horizontal,
            SliderOrientation.from(
                isVertical = true,
                sliderPosition = ReaderSliderPosition.HORIZONTAL,
            ),
        )
    }

    @Test
    fun `given default ReaderSliderUiState, verify default properties`() {
        val state = ReaderSliderUiState()

        assertEquals("", state.currentPageText)
        assertEquals("", state.totalPagesText)
        assertEquals(0, state.currentPageIndex)
        assertEquals(0, state.totalPages)
        assertFalse(state.isRtl)
        assertEquals(SliderOrientation.Horizontal, state.position)
    }

    @Test
    fun `given ReaderSliderUiState, when copied with new page, preserves other properties`() {
        val initial =
            ReaderSliderUiState(
                currentPageText = "1",
                totalPagesText = "24",
                currentPageIndex = 0,
                totalPages = 23,
                isRtl = true,
                position = SliderOrientation.VerticalRight,
            )

        val updated =
            initial.copy(
                currentPageText = "2",
                currentPageIndex = 1,
            )

        assertEquals("2", updated.currentPageText)
        assertEquals("24", updated.totalPagesText)
        assertEquals(1, updated.currentPageIndex)
        assertEquals(23, updated.totalPages)
        assertTrue(updated.isRtl)
        assertEquals(SliderOrientation.VerticalRight, updated.position)
        assertNotEquals(initial, updated)
    }

    @Test
    fun `given identical ReaderSliderUiState, equals and hashCode contract is satisfied`() {
        val state1 =
            ReaderSliderUiState(
                currentPageText = "5",
                totalPagesText = "10",
                currentPageIndex = 4,
                totalPages = 9,
                isRtl = false,
                position = SliderOrientation.VerticalLeft,
            )
        val state2 =
            ReaderSliderUiState(
                currentPageText = "5",
                totalPagesText = "10",
                currentPageIndex = 4,
                totalPages = 9,
                isRtl = false,
                position = SliderOrientation.VerticalLeft,
            )

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun `given ReaderToolbarButtonUiModel with iconRes, verify properties`() {
        val button =
            ReaderToolbarButtonUiModel(
                id = ReaderBottomActionId.Chapters,
                iconRes = R.drawable.ic_format_list_numbered_24dp,
                tooltipRes = R.string.view_chapters,
                isToggled = false,
                isVisible = true,
            )

        assertEquals(ReaderBottomActionId.Chapters, button.id)
        assertEquals(R.drawable.ic_format_list_numbered_24dp, button.iconRes)
        assertEquals(R.string.view_chapters, button.tooltipRes)
        assertNull(button.iconVector)
        assertFalse(button.isToggled)
        assertTrue(button.isVisible)
    }

    @Test
    fun `given ReaderToolbarButtonUiModel with iconVector, verify properties`() {
        val vector = Icons.Default.Crop
        val button =
            ReaderToolbarButtonUiModel(
                id = ReaderBottomActionId.CropBorders,
                iconVector = vector,
                tooltipRes = R.string.crop_borders,
                isToggled = true,
                isVisible = false,
            )

        assertEquals(ReaderBottomActionId.CropBorders, button.id)
        assertEquals(0, button.iconRes)
        assertSame(vector, button.iconVector)
        assertEquals(R.string.crop_borders, button.tooltipRes)
        assertTrue(button.isToggled)
        assertFalse(button.isVisible)
    }

    @Test
    fun `given identical ReaderToolbarButtonUiModel, equals and hashCode contract is satisfied`() {
        val btn1 =
            ReaderToolbarButtonUiModel(
                id = ReaderBottomActionId.Settings,
                iconRes = R.drawable.ic_tune_24dp,
                tooltipRes = R.string.display_options,
            )
        val btn2 =
            ReaderToolbarButtonUiModel(
                id = ReaderBottomActionId.Settings,
                iconRes = R.drawable.ic_tune_24dp,
                tooltipRes = R.string.display_options,
            )

        assertEquals(btn1, btn2)
        assertEquals(btn1.hashCode(), btn2.hashCode())
    }

    @Test
    fun `given default ReaderBottomControlsUiState, verify defaults`() {
        val state = ReaderBottomControlsUiState()

        assertFalse(state.isVisible)
        assertFalse(state.isLoading)
        assertEquals(ReaderSliderUiState(), state.sliderState)
        assertTrue(state.buttons.isEmpty())
    }

    @Test
    fun `given ReaderBottomControlsUiState with content, when copied, maintains immutability`() {
        val buttons = buildReaderBottomBarButtons()
        val slider =
            ReaderSliderUiState(
                currentPageText = "3",
                totalPagesText = "15",
                currentPageIndex = 2,
                totalPages = 14,
            )

        val initial =
            ReaderBottomControlsUiState(
                isVisible = true,
                isLoading = true,
                sliderState = slider,
                buttons = buttons,
            )

        val updated = initial.copy(isLoading = false)

        assertTrue(updated.isVisible)
        assertFalse(updated.isLoading)
        assertEquals(slider, updated.sliderState)
        assertEquals(buttons, updated.buttons)
        assertNotEquals(initial, updated)
    }

    @Test
    fun `given ReaderBottomBarAction types, verify sealed interface contracts`() {
        val pageAction = ReaderBottomBarAction.PageChanged(pageIndex = 7)
        assertEquals(7, pageAction.pageIndex)

        val prevAction: ReaderBottomBarAction = ReaderBottomBarAction.SkipPrevious
        val nextAction: ReaderBottomBarAction = ReaderBottomBarAction.SkipNext
        assertSame(ReaderBottomBarAction.SkipPrevious, prevAction)
        assertSame(ReaderBottomBarAction.SkipNext, nextAction)

        val buttonAction =
            ReaderBottomBarAction.ButtonClicked(actionId = ReaderBottomActionId.ReadingMode)
        assertEquals(ReaderBottomActionId.ReadingMode, buttonAction.actionId)
    }

    @Test
    fun `given default buildReaderBottomBarButtons, returns 10 buttons with correct ids and tooltips in order`() {
        val buttons = buildReaderBottomBarButtons()

        assertEquals(10, buttons.size)

        val expectedIds =
            listOf(
                ReaderBottomActionId.Chapters,
                ReaderBottomActionId.Comments,
                ReaderBottomActionId.WebView,
                ReaderBottomActionId.ReadingMode,
                ReaderBottomActionId.Rotation,
                ReaderBottomActionId.CropBorders,
                ReaderBottomActionId.Grayscale,
                ReaderBottomActionId.DoublePage,
                ReaderBottomActionId.ShiftPage,
                ReaderBottomActionId.Settings,
            )

        val expectedTooltips =
            listOf(
                R.string.view_chapters,
                R.string.comments,
                R.string.open_in_webview,
                R.string.reading_mode,
                R.string.rotation,
                R.string.crop_borders,
                R.string.grayscale_toggle,
                R.string.double_pages,
                R.string.shift_one_page_over,
                R.string.display_options,
            )

        assertEquals(expectedIds, buttons.map { it.id })
        assertEquals(expectedTooltips, buttons.map { it.tooltipRes })
    }

    @Test
    fun `given default buildReaderBottomBarButtons, verify default visibility flags`() {
        val buttons = buildReaderBottomBarButtons().associate { it.id to it.isVisible }

        assertTrue(buttons[ReaderBottomActionId.Chapters] == true)
        assertTrue(buttons[ReaderBottomActionId.Comments] == true)
        assertTrue(buttons[ReaderBottomActionId.WebView] == true)
        assertFalse(buttons[ReaderBottomActionId.ReadingMode] == true)
        assertFalse(buttons[ReaderBottomActionId.Rotation] == true)
        assertFalse(buttons[ReaderBottomActionId.CropBorders] == true)
        assertFalse(buttons[ReaderBottomActionId.Grayscale] == true)
        assertFalse(buttons[ReaderBottomActionId.DoublePage] == true)
        assertFalse(buttons[ReaderBottomActionId.ShiftPage] == true)
        assertTrue(buttons[ReaderBottomActionId.Settings] == true)
    }

    @Test
    fun `given toggled state, buildReaderBottomBarButtons maps toggle flags and crop vector correctly`() {
        val buttonsUntoggled =
            buildReaderBottomBarButtons(
                cropBorders = false,
                grayscale = false,
                isDoublePage = false,
            )

        val cropBtnUntoggled = buttonsUntoggled.first { it.id == ReaderBottomActionId.CropBorders }
        assertFalse(cropBtnUntoggled.isToggled)
        assertEquals(Icons.Default.Crop, cropBtnUntoggled.iconVector)

        val grayBtnUntoggled = buttonsUntoggled.first { it.id == ReaderBottomActionId.Grayscale }
        assertFalse(grayBtnUntoggled.isToggled)

        val doublePageBtnUntoggled = buttonsUntoggled.first {
            it.id == ReaderBottomActionId.DoublePage
        }
        assertFalse(doublePageBtnUntoggled.isToggled)

        val buttonsToggled =
            buildReaderBottomBarButtons(
                cropBorders = true,
                grayscale = true,
                isDoublePage = true,
            )

        val cropBtnToggled = buttonsToggled.first { it.id == ReaderBottomActionId.CropBorders }
        assertTrue(cropBtnToggled.isToggled)
        assertEquals(Icons.Default.CropFree, cropBtnToggled.iconVector)

        val grayBtnToggled = buttonsToggled.first { it.id == ReaderBottomActionId.Grayscale }
        assertTrue(grayBtnToggled.isToggled)

        val doublePageBtnToggled = buttonsToggled.first { it.id == ReaderBottomActionId.DoublePage }
        assertTrue(doublePageBtnToggled.isToggled)
    }

    @Test
    fun `given custom icons, buildReaderBottomBarButtons assigns icon resources correctly`() {
        val buttons =
            buildReaderBottomBarButtons(
                readingModeIconRes = R.drawable.ic_reader_webtoon_24dp,
                rotationIconRes = R.drawable.ic_screen_rotation_24dp,
                doublePageIconRes = R.drawable.ic_book_open_split_24dp,
                shiftPageIconRes = R.drawable.ic_page_previous_outline_24dp,
            )

        assertEquals(
            R.drawable.ic_reader_webtoon_24dp,
            buttons.first { it.id == ReaderBottomActionId.ReadingMode }.iconRes,
        )
        assertEquals(
            R.drawable.ic_screen_rotation_24dp,
            buttons.first { it.id == ReaderBottomActionId.Rotation }.iconRes,
        )
        assertEquals(
            R.drawable.ic_book_open_split_24dp,
            buttons.first { it.id == ReaderBottomActionId.DoublePage }.iconRes,
        )
        assertEquals(
            R.drawable.ic_page_previous_outline_24dp,
            buttons.first { it.id == ReaderBottomActionId.ShiftPage }.iconRes,
        )
    }

    @Test
    fun `given custom visibility flags, buildReaderBottomBarButtons updates isVisible accordingly`() {
        val buttons =
            buildReaderBottomBarButtons(
                    isChaptersVisible = false,
                    isCommentsVisible = false,
                    isWebViewVisible = false,
                    isReadingModeVisible = true,
                    isRotationVisible = true,
                    isCropBordersVisible = true,
                    isGrayscaleVisible = true,
                    isDoublePageVisible = true,
                    isShiftPageVisible = true,
                    isSettingsVisible = false,
                )
                .associate { it.id to it.isVisible }

        assertFalse(buttons[ReaderBottomActionId.Chapters] == true)
        assertFalse(buttons[ReaderBottomActionId.Comments] == true)
        assertFalse(buttons[ReaderBottomActionId.WebView] == true)
        assertTrue(buttons[ReaderBottomActionId.ReadingMode] == true)
        assertTrue(buttons[ReaderBottomActionId.Rotation] == true)
        assertTrue(buttons[ReaderBottomActionId.CropBorders] == true)
        assertTrue(buttons[ReaderBottomActionId.Grayscale] == true)
        assertTrue(buttons[ReaderBottomActionId.DoublePage] == true)
        assertTrue(buttons[ReaderBottomActionId.ShiftPage] == true)
        assertFalse(buttons[ReaderBottomActionId.Settings] == true)
    }

    @Test
    fun `given ReaderButtonIcon Resource, verify properties and equality`() {
        val icon1 = ReaderButtonIcon.Resource(R.drawable.ic_format_list_numbered_24dp)
        val icon2 = ReaderButtonIcon.Resource(R.drawable.ic_format_list_numbered_24dp)
        val icon3 = ReaderButtonIcon.Resource(R.drawable.ic_view_comments_24p)

        assertEquals(R.drawable.ic_format_list_numbered_24dp, icon1.id)
        assertEquals(icon1, icon2)
        assertEquals(icon1.hashCode(), icon2.hashCode())
        assertNotEquals(icon1, icon3)
    }

    @Test
    fun `given ReaderButtonIcon Vector, verify properties and equality`() {
        val icon1 = ReaderButtonIcon.Vector(Icons.Default.Crop)
        val icon2 = ReaderButtonIcon.Vector(Icons.Default.Crop)
        val icon3 = ReaderButtonIcon.Vector(Icons.Default.CropFree)

        assertSame(Icons.Default.Crop, icon1.imageVector)
        assertEquals(icon1, icon2)
        assertEquals(icon1.hashCode(), icon2.hashCode())
        assertNotEquals(icon1, icon3)
    }

    @Test
    fun `given ReaderToolbarButtonUiModel with primary constructor, verify icon and toggleStyle`() {
        val button =
            ReaderToolbarButtonUiModel(
                id = ReaderBottomActionId.CropBorders,
                icon = ReaderButtonIcon.Vector(Icons.Default.Crop),
                tooltipRes = R.string.crop_borders,
                toggleStyle = ButtonToggleStyle.PrimaryWhenToggled,
                isToggled = true,
                isVisible = true,
            )

        assertEquals(ReaderBottomActionId.CropBorders, button.id)
        assertEquals(ReaderButtonIcon.Vector(Icons.Default.Crop), button.icon)
        assertEquals(Icons.Default.Crop, button.iconVector)
        assertEquals(0, button.iconRes)
        assertEquals(ButtonToggleStyle.PrimaryWhenToggled, button.toggleStyle)
        assertTrue(button.isToggled)
        assertTrue(button.isVisible)
    }

    @Test
    fun `given buildReaderBottomBarButtons, verify ButtonToggleStyle configuration`() {
        val buttons = buildReaderBottomBarButtons()

        val cropButton = buttons.first { it.id == ReaderBottomActionId.CropBorders }
        val grayButton = buttons.first { it.id == ReaderBottomActionId.Grayscale }
        assertEquals(ButtonToggleStyle.PrimaryWhenToggled, cropButton.toggleStyle)
        assertEquals(ButtonToggleStyle.PrimaryWhenToggled, grayButton.toggleStyle)

        val otherButtons = buttons.filter {
            it.id != ReaderBottomActionId.CropBorders && it.id != ReaderBottomActionId.Grayscale
        }
        otherButtons.forEach { btn ->
            assertEquals(
                "Button ${btn.id} should have ButtonToggleStyle.None",
                ButtonToggleStyle.None,
                btn.toggleStyle,
            )
        }
    }
}
