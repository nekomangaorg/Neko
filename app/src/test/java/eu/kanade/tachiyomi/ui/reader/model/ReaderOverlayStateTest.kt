package eu.kanade.tachiyomi.ui.reader.model

import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.reader.NavigationRegionUiModel

class ReaderOverlayStateTest {

    @Test
    fun `given default ReaderViewModel State, overlay fields have expected defaults`() {
        val state = ReaderViewModel.State()

        assertNull(state.overlayNavigation)
        assertFalse(state.overlayVisible)
        assertTrue(state.overlayIsLtr)
        assertEquals(ViewerNavigation.TappingInvertMode.NONE, state.overlayInvertMode)
        assertTrue(state.overlayRegions.isEmpty())
    }

    @Test
    fun `given ReaderViewModel State, when updating overlay fields via copy, state preserves immutability`() {
        val nav = KindlishNavigation()
        val regions =
            listOf(
                NavigationRegionUiModel(
                    id = "NEXT_0.33_0.33_1.0_1.0",
                    leftFraction = 0.33f,
                    topFraction = 0.33f,
                    rightFraction = 1f,
                    bottomFraction = 1f,
                    label = UiText.StringResource(R.string.next),
                    color = Color(0xCCFF124A),
                )
            )

        val initial = ReaderViewModel.State()
        val updated =
            initial.copy(
                overlayNavigation = nav,
                overlayVisible = true,
                overlayIsLtr = false,
                overlayInvertMode = ViewerNavigation.TappingInvertMode.BOTH,
                overlayRegions = regions,
            )

        assertEquals(nav, updated.overlayNavigation)
        assertTrue(updated.overlayVisible)
        assertFalse(updated.overlayIsLtr)
        assertEquals(ViewerNavigation.TappingInvertMode.BOTH, updated.overlayInvertMode)
        assertEquals(regions, updated.overlayRegions)

        // Verify initial remains untouched
        assertNull(initial.overlayNavigation)
        assertFalse(initial.overlayVisible)
        assertTrue(initial.overlayRegions.isEmpty())
    }

    @Test
    fun `given ReaderViewModel State with regions, copying with new visibility retains regions`() {
        val region =
            NavigationRegionUiModel(
                id = "PREV_0.0_0.33_0.33_1.0",
                leftFraction = 0f,
                topFraction = 0.33f,
                rightFraction = 0.33f,
                bottomFraction = 1f,
                label = UiText.StringResource(R.string.previous),
                color = Color(0xCCFFD9DF),
            )

        val stateWithOverlay =
            ReaderViewModel.State(
                overlayVisible = true,
                overlayRegions = listOf(region),
            )

        val dismissedState = stateWithOverlay.copy(overlayVisible = false)

        assertFalse(dismissedState.overlayVisible)
        assertEquals(1, dismissedState.overlayRegions.size)
        assertEquals(region, dismissedState.overlayRegions[0])
    }

    @Test
    fun `given differing overlay properties, States are not equal`() {
        val baseState = ReaderViewModel.State()
        val stateWithNav = baseState.copy(overlayNavigation = KindlishNavigation())
        val stateWithVisibility = baseState.copy(overlayVisible = true)
        val stateWithRtl = baseState.copy(overlayIsLtr = false)
        val stateWithInvert =
            baseState.copy(overlayInvertMode = ViewerNavigation.TappingInvertMode.HORIZONTAL)
        val stateWithRegions =
            baseState.copy(
                overlayRegions =
                    listOf(
                        NavigationRegionUiModel(
                            id = "TEST",
                            leftFraction = 0f,
                            topFraction = 0f,
                            rightFraction = 1f,
                            bottomFraction = 1f,
                            label = UiText.String("Test"),
                            color = Color.Red,
                        )
                    )
            )

        assertNotEquals(baseState, stateWithNav)
        assertNotEquals(baseState, stateWithVisibility)
        assertNotEquals(baseState, stateWithRtl)
        assertNotEquals(baseState, stateWithInvert)
        assertNotEquals(baseState, stateWithRegions)
    }
}
