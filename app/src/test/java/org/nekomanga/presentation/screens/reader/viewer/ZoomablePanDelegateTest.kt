package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPanDelegate
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

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = testScope,
            )

        // Case 1: unzoomed fit to screen
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        assertFalse(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertFalse(delegate.canPanUp())
        assertFalse(delegate.canPanDown())

        // Case 2: zoomed in, horizontally overflowing on both sides
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = -200f, top = 0f, right = 1200f, bottom = 2000f)
        assertTrue(delegate.canPanLeft())
        assertTrue(delegate.canPanRight())
        assertFalse(delegate.canPanUp())
        assertFalse(delegate.canPanDown())

        // Case 3: zoomed in, vertically overflowing on both sides
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = 0f, top = -300f, right = 1000f, bottom = 2300f)
        assertFalse(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertTrue(delegate.canPanUp())
        assertTrue(delegate.canPanDown())

        // Case 4: zoomed in and panned to extreme bottom-right edge
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = -500f, top = -600f, right = 1000f, bottom = 2000f)
        assertTrue(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertTrue(delegate.canPanUp())
        assertFalse(delegate.canPanDown())
    }

    @Test
    fun `panRight pans zoomableState by negative offset within remaining bounds`() = runTest {
        val mockZoomableState = mockk<ZoomableState>(relaxed = true)
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = 0f, top = 0f, right = 1500f, bottom = 2000f)

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
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
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = -400f, top = 0f, right = 1000f, bottom = 2000f)

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
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
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = 0f, top = 0f, right = 1000f, bottom = 2800f)

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
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
        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns
            Rect(left = 0f, top = -600f, right = 1000f, bottom = 2000f)

        val delegate =
            ZoomablePanDelegate(
                zoomableState = mockZoomableState,
                viewportWidthPx = 1000f,
                viewportHeightPx = 2000f,
                scope = this,
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
            )

        @Suppress("DEPRECATION")
        every { mockZoomableState.transformedContentBounds } returns Rect.Zero

        assertFalse(delegate.canPanLeft())
        assertFalse(delegate.canPanRight())
        assertFalse(delegate.canPanUp())
        assertFalse(delegate.canPanDown())
    }

    @Test
    fun `given empty bounds when pan methods called then no panBy animation is dispatched`() =
        runTest {
            val mockZoomableState = mockk<ZoomableState>(relaxed = true)
            @Suppress("DEPRECATION")
            every { mockZoomableState.transformedContentBounds } returns Rect.Zero

            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
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
            // Exceeds viewport by 0.5px (below the 1f minimum threshold)
            @Suppress("DEPRECATION")
            every { mockZoomableState.transformedContentBounds } returns
                Rect(left = -0.5f, top = -0.5f, right = 1000.5f, bottom = 2000.5f)

            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
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
            // Content is 5x wider than screen, remaining pan to right is 4000px
            @Suppress("DEPRECATION")
            every { mockZoomableState.transformedContentBounds } returns
                Rect(left = 0f, top = 0f, right = 5000f, bottom = 2000f)

            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
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
            // Content is 5x wider than screen, remaining pan to left is 4000px
            @Suppress("DEPRECATION")
            every { mockZoomableState.transformedContentBounds } returns
                Rect(left = -4000f, top = 0f, right = 1000f, bottom = 2000f)

            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
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
            // Content is 4x taller than screen, remaining pan down is 6000px
            @Suppress("DEPRECATION")
            every { mockZoomableState.transformedContentBounds } returns
                Rect(left = 0f, top = 0f, right = 1000f, bottom = 8000f)

            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
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
            // Content is 4x taller than screen, remaining pan up is 6000px
            @Suppress("DEPRECATION")
            every { mockZoomableState.transformedContentBounds } returns
                Rect(left = 0f, top = -6000f, right = 1000f, bottom = 2000f)

            val delegate =
                ZoomablePanDelegate(
                    zoomableState = mockZoomableState,
                    viewportWidthPx = 1000f,
                    viewportHeightPx = 2000f,
                    scope = this,
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
}
