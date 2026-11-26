package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebtoonZoomableModifierTest {

    private class TestFrameClock : MonotonicFrameClock {
        override val key: CoroutineContext.Key<*> = MonotonicFrameClock
        private var timeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            timeNanos += 16_000_000L
            return onFrame(timeNanos)
        }
    }

    @Test
    fun `calculateZoomScrollConsumption returns zero when scale is 1 or at threshold`() {
        assertEquals(0f, calculateZoomScrollConsumption(100f, 1f), 0.001f)
        assertEquals(0f, calculateZoomScrollConsumption(100f, ZOOM_INTERACTION_THRESHOLD), 0.001f)
    }

    @Test
    fun `calculateZoomScrollConsumption returns zero when scale is less than threshold`() {
        assertEquals(0f, calculateZoomScrollConsumption(100f, 1.005f), 0.001f)
        assertEquals(0f, calculateZoomScrollConsumption(100f, 0.5f), 0.001f)
        assertEquals(0f, calculateZoomScrollConsumption(100f, 0f), 0.001f)
        assertEquals(0f, calculateZoomScrollConsumption(100f, -1f), 0.001f)
    }

    @Test
    fun `calculateZoomScrollConsumption consumes half the delta when scale is 2`() {
        // At scale 2, LazyColumn should receive delta / 2 = 50f so that visually 50f * 2 = 100f.
        // Therefore, consumed should be 100f - 50f = 50f.
        assertEquals(50f, calculateZoomScrollConsumption(100f, 2f), 0.001f)
        assertEquals(-50f, calculateZoomScrollConsumption(-100f, 2f), 0.001f)
    }

    @Test
    fun `calculateZoomScrollConsumption consumes sixty percent of delta when scale is 2_5`() {
        // At scale 2.5, LazyColumn should receive delta / 2.5 = 40f so that visually 40f * 2.5 =
        // 100f.
        // Therefore, consumed should be 100f - 40f = 60f.
        assertEquals(60f, calculateZoomScrollConsumption(100f, 2.5f), 0.001f)
        assertEquals(-60f, calculateZoomScrollConsumption(-100f, 2.5f), 0.001f)
    }

    @Test
    fun `WebtoonZoomScrollConnection leaves a zoomed drag to the LazyColumn`() {
        // The LazyColumn sits inside the zoom layer, so a drag reaches it already divided by the
        // scale and moves the page as far as the finger
        val state = WebtoonZoomState(initialScale = 2.5f)

        val consumed =
            state.scrollConnection.onPreScroll(
                Offset(x = 25f, y = 100f),
                NestedScrollSource.UserInput,
            )

        assertEquals(Offset.Zero, consumed)
    }

    @Test
    fun `WebtoonZoomScrollConnection onPreFling scales vertical velocity when zoomed in`() =
        runTest {
            val state = WebtoonZoomState(initialScale = 2f)
            val connection = state.scrollConnection

            val available = Velocity(x = 500f, y = 1000f)
            val consumed = connection.onPreFling(available)

            assertEquals(0f, consumed.x, 0.001f)
            assertEquals(500f, consumed.y, 0.001f)
        }

    @Test
    fun `WebtoonZoomScrollConnection onPreFling does not consume when scale is 1`() = runTest {
        val state = WebtoonZoomState(initialScale = 1f)
        val connection = state.scrollConnection

        val available = Velocity(x = 500f, y = 1000f)
        val consumed = connection.onPreFling(available)

        assertEquals(Velocity.Zero, consumed)
    }

    @Test
    fun `WebtoonZoomState reset restores scale and offset`() = runTest {
        val state = WebtoonZoomState(initialScale = 2.5f, initialOffsetX = 100f)
        state.reset()

        assertEquals(1f, state.scale, 0.001f)
        assertEquals(0f, state.offsetX, 0.001f)
    }

    @Test
    fun `panBy moves the page as far as the finger moved on screen`() {
        // At 2.5x a finger that moves 250 px on screen reports 100 px inside the zoomed layer
        val state = WebtoonZoomState(initialScale = 2.5f)

        state.panBy(deltaX = -100f, viewportWidth = 1080f)

        assertEquals(-250f, state.offsetX, 0.001f)
    }

    @Test
    fun `panBy stops at the page edge`() {
        // 2.5x on a 1080 px viewport leaves 1080 * 1.5 / 2 = 810 px to each side
        val state = WebtoonZoomState(initialScale = 2.5f, initialOffsetX = 800f)

        state.panBy(deltaX = 100f, viewportWidth = 1080f)

        assertEquals(810f, state.offsetX, 0.001f)
    }

    @Test
    fun `WebtoonZoomState flingHorizontal animates offset and cancelFling arrests animation`() =
        runTest {
            withContext(TestFrameClock()) {
                val state = WebtoonZoomState(initialScale = 2f, initialOffsetX = 0f)
                state.flingHorizontal(
                    coroutineScope = this,
                    velocityX = 1000f,
                    maxOffsetX = 500f,
                )

                advanceTimeBy(150)
                assertTrue(state.offsetX > 0f)

                state.cancelFling()
                val offsetAfterCancel = state.offsetX

                advanceTimeBy(300)
                // Offset should not advance further after fling is cancelled
                assertEquals(offsetAfterCancel, state.offsetX, 0.001f)
            }
        }

    @Test
    fun `WebtoonZoomState successive flingHorizontal calls supersede previous fling cleanly`() =
        runTest {
            withContext(TestFrameClock()) {
                val state = WebtoonZoomState(initialScale = 2f, initialOffsetX = 0f)
                state.flingHorizontal(
                    coroutineScope = this,
                    velocityX = 1000f,
                    maxOffsetX = 500f,
                )

                advanceTimeBy(100)
                val intermediateOffset = state.offsetX

                // Reverse fling
                state.flingHorizontal(
                    coroutineScope = this,
                    velocityX = -2000f,
                    maxOffsetX = 500f,
                )

                advanceTimeBy(300)
                // Must have reversed direction past the intermediate offset
                assertTrue(state.offsetX < intermediateOffset)
            }
        }
}
