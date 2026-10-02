package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.animation.core.SnapSpec
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPanDelegate
import eu.kanade.tachiyomi.ui.reader.viewer.pager.tryStepPan
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.saket.telephoto.zoomable.ZoomableState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZoomablePanDelegateTest {

    @Test
    fun `PagerPanDelegate default interface implementation behaves safely`() {
        val defaultDelegate = object : PagerPanDelegate {}

        assertFalse(defaultDelegate.canPanLeft())
        assertFalse(defaultDelegate.canPanRight())
        assertFalse(defaultDelegate.canPanUp())
        assertFalse(defaultDelegate.canPanDown())

        // Default actions should be no-ops and not throw exceptions
        defaultDelegate.panLeft()
        defaultDelegate.panRight()
        defaultDelegate.panUp()
        defaultDelegate.panDown()
    }

    @Test
    fun `canPan directions evaluate bounds correctly against viewport dimensions`() {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val testScope = TestScope()
        var currentBounds = Rect.Zero

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = testScope,
                contentBoundsProvider = { currentBounds },
            )

        // Case 1: unzoomed fit to screen
        currentBounds = Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        assertFalse(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertFalse(delegate.canPanUp())
        assertFalse(delegate.canPanDown())

        // Case 2: zoomed in, horizontally overflowing on both sides
        currentBounds = Rect(left = -200f, top = 0f, right = 1200f, bottom = 2000f)
        assertTrue(delegate.canPanLeft())
        assertTrue(delegate.canPanRight())
        assertFalse(delegate.canPanUp())
        assertFalse(delegate.canPanDown())

        // Case 3: zoomed in, vertically overflowing on both sides
        currentBounds = Rect(left = 0f, top = -300f, right = 1000f, bottom = 2300f)
        assertFalse(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertTrue(delegate.canPanUp())
        assertTrue(delegate.canPanDown())

        // Case 4: zoomed in and panned to extreme bottom-right edge
        currentBounds = Rect(left = -500f, top = -600f, right = 1000f, bottom = 2000f)
        assertTrue(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertTrue(delegate.canPanUp())
        assertFalse(delegate.canPanDown())
    }

    @Test
    fun `panRight pans zoomableState by negative offset within remaining bounds`() = runTest {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
                contentBoundsProvider = {
                    Rect(left = 0f, top = 0f, right = 1500f, bottom = 2000f)
                },
            )

        delegate.panRight()
        runCurrent()

        coVerify {
            mockZoomableState.panBy(
                offset = Offset(x = -500f, y = 0f),
                animationSpec = any(),
            )
        }
    }

    @Test
    fun `panLeft pans zoomableState by positive offset within remaining bounds`() = runTest {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
                contentBoundsProvider = {
                    Rect(left = -400f, top = 0f, right = 1000f, bottom = 2000f)
                },
            )

        delegate.panLeft()
        runCurrent()

        coVerify {
            mockZoomableState.panBy(
                offset = Offset(x = 400f, y = 0f),
                animationSpec = any(),
            )
        }
    }

    @Test
    fun `panDown pans zoomableState by negative offset within remaining bounds`() = runTest {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
                contentBoundsProvider = {
                    Rect(left = 0f, top = 0f, right = 1000f, bottom = 2800f)
                },
            )

        delegate.panDown()
        runCurrent()

        coVerify {
            mockZoomableState.panBy(
                offset = Offset(x = 0f, y = -800f),
                animationSpec = any(),
            )
        }
    }

    @Test
    fun `panUp pans zoomableState by positive offset within remaining bounds`() = runTest {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
                contentBoundsProvider = {
                    Rect(left = 0f, top = -600f, right = 1000f, bottom = 2000f)
                },
            )

        delegate.panUp()
        runCurrent()

        coVerify {
            mockZoomableState.panBy(
                offset = Offset(x = 0f, y = 600f),
                animationSpec = any(),
            )
        }
    }

    @Test
    fun `given empty bounds when checking canPan directions then all return false`() {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val testScope = TestScope()

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = testScope,
                contentBoundsProvider = { Rect.Zero },
            )

        assertFalse(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertFalse(delegate.canPanUp())
        assertFalse(delegate.canPanDown())
    }

    @Test
    fun `given empty bounds when pan methods called then no panBy animation is dispatched`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
                    contentBoundsProvider = { Rect.Zero },
                )

            delegate.panLeft()
            delegate.panRight()
            delegate.panUp()
            delegate.panDown()
            runCurrent()

            coVerify(exactly = 0) {
                mockZoomableState.panBy(offset = any<Offset>(), animationSpec = any())
            }
        }

    @Test
    fun `given bounds exceeding viewport by less than or equal to 1px when panning then panBy is not dispatched`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
                    contentBoundsProvider = {
                        Rect(left = -0.5f, top = -0.5f, right = 1000.5f, bottom = 2000.5f)
                    },
                )

            delegate.panLeft()
            delegate.panRight()
            delegate.panUp()
            delegate.panDown()
            runCurrent()

            coVerify(exactly = 0) {
                mockZoomableState.panBy(offset = any<Offset>(), animationSpec = any())
            }
        }

    @Test
    fun `given massive zoom overflowing viewport by multiple screens when panRight called then pan amount is capped to viewportWidthPx`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
                    contentBoundsProvider = {
                        Rect(left = 0f, top = 0f, right = 5000f, bottom = 2000f)
                    },
                )

            delegate.panRight()
            runCurrent()

            // Should cap at 1 screen width (-1000px) instead of jumping 4000px at once
            coVerify {
                mockZoomableState.panBy(
                    offset = Offset(x = -1000f, y = 0f),
                    animationSpec = any(),
                )
            }
        }

    @Test
    fun `given massive zoom overflowing viewport by multiple screens when panLeft called then pan amount is capped to viewportWidthPx`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
                    contentBoundsProvider = {
                        Rect(left = -4000f, top = 0f, right = 1000f, bottom = 2000f)
                    },
                )

            delegate.panLeft()
            runCurrent()

            // Should cap at 1 screen width (1000px) instead of jumping 4000px at once
            coVerify {
                mockZoomableState.panBy(
                    offset = Offset(x = 1000f, y = 0f),
                    animationSpec = any(),
                )
            }
        }

    @Test
    fun `given massive zoom overflowing viewport by multiple screens when panDown called then pan amount is capped to viewportHeightPx`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
                    contentBoundsProvider = {
                        Rect(left = 0f, top = 0f, right = 1000f, bottom = 8000f)
                    },
                )

            delegate.panDown()
            runCurrent()

            // Should cap at 1 screen height (-2000px)
            coVerify {
                mockZoomableState.panBy(
                    offset = Offset(x = 0f, y = -2000f),
                    animationSpec = any(),
                )
            }
        }

    @Test
    fun `given massive zoom overflowing viewport by multiple screens when panUp called then pan amount is capped to viewportHeightPx`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
                    contentBoundsProvider = {
                        Rect(left = 0f, top = -6000f, right = 1000f, bottom = 2000f)
                    },
                )

            delegate.panUp()
            runCurrent()

            // Should cap at 1 screen height (2000px)
            coVerify {
                mockZoomableState.panBy(
                    offset = Offset(x = 0f, y = 2000f),
                    animationSpec = any(),
                )
            }
        }

    @Test
    fun `when animated is false panBy uses SnapSpec`() = runTest {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
                animated = false,
                contentBoundsProvider = {
                    Rect(left = 0f, top = 0f, right = 1500f, bottom = 2000f)
                },
            )

        delegate.panRight()
        runCurrent()

        coVerify {
            mockZoomableState.panBy(
                offset = Offset(x = -500f, y = 0f),
                animationSpec = match { it is SnapSpec },
            )
        }
    }

    @Test
    fun `custom panEpsilonPx prevents micro-jitter edge transitions`() {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        var currentBounds = Rect(left = -7f, top = 0f, right = 1007f, bottom = 2000f)

        // With standard 5f epsilon: 7f > 5f -> canPanLeft & canPanRight are true
        val standardDelegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = TestScope(),
                panEpsilonPx = 5f,
                contentBoundsProvider = { currentBounds },
            )
        assertTrue(standardDelegate.canPanLeft())
        assertTrue(standardDelegate.canPanRight())

        // With high-density 10f epsilon: 7f < 10f -> canPanLeft & canPanRight evaluate to false
        val highDensityDelegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = TestScope(),
                panEpsilonPx = 10f,
                contentBoundsProvider = { currentBounds },
            )
        assertFalse(highDensityDelegate.canPanLeft())
        assertFalse(highDensityDelegate.canPanRight())
    }

    @Test
    fun `tryStepPan resolves correct panning direction and executes pan`() {
        val mockDelegate = mockk<PagerPanDelegate>(relaxed = true)

        // 1. Vertical forward -> panDown
        every { mockDelegate.canPanDown() } returns true
        assertTrue(mockDelegate.tryStepPan(isVertical = true, isRtl = false, forward = true))
        coVerify(exactly = 1) { mockDelegate.panDown() }

        // 2. Vertical backward -> panUp
        every { mockDelegate.canPanUp() } returns true
        assertTrue(mockDelegate.tryStepPan(isVertical = true, isRtl = false, forward = false))
        coVerify(exactly = 1) { mockDelegate.panUp() }

        // 3. RTL forward -> panLeft
        every { mockDelegate.canPanLeft() } returns true
        assertTrue(mockDelegate.tryStepPan(isVertical = false, isRtl = true, forward = true))
        coVerify(exactly = 1) { mockDelegate.panLeft() }

        // 4. RTL backward -> panRight
        every { mockDelegate.canPanRight() } returns true
        assertTrue(mockDelegate.tryStepPan(isVertical = false, isRtl = true, forward = false))
        coVerify(exactly = 1) { mockDelegate.panRight() }

        // 5. LTR forward -> panRight
        every { mockDelegate.canPanRight() } returns true
        assertTrue(mockDelegate.tryStepPan(isVertical = false, isRtl = false, forward = true))
        coVerify(exactly = 2) { mockDelegate.panRight() }

        // 6. LTR backward -> panLeft
        every { mockDelegate.canPanLeft() } returns true
        assertTrue(mockDelegate.tryStepPan(isVertical = false, isRtl = false, forward = false))
        coVerify(exactly = 2) { mockDelegate.panLeft() }

        // 7. When boundary is reached (cannot pan), returns false without panning
        every { mockDelegate.canPanRight() } returns false
        assertFalse(mockDelegate.tryStepPan(isVertical = false, isRtl = false, forward = true))
        coVerify(exactly = 2) { mockDelegate.panRight() }
    }
}
