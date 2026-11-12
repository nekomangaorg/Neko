package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WebtoonZoomableModifierTest {

    @Test
    fun `calculateZoomScrollConsumption returns zero when scale is 1`() {
        assertEquals(0f, calculateZoomScrollConsumption(100f, 1f), 0.001f)
    }

    @Test
    fun `calculateZoomScrollConsumption returns zero when scale is less than 1`() {
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
    fun `WebtoonZoomScrollConnection onPreScroll consumes vertical delta for UserInput when zoomed in`() {
        val state = WebtoonZoomState(initialScale = 2f)
        val connection = state.scrollConnection

        val available = Offset(x = 25f, y = 100f)
        val consumed = connection.onPreScroll(available, NestedScrollSource.UserInput)

        // Only Y delta is consumed; X is unconsumed so horizontal pan gestures handle it
        assertEquals(0f, consumed.x, 0.001f)
        assertEquals(50f, consumed.y, 0.001f)
    }

    @Test
    fun `WebtoonZoomScrollConnection onPreScroll does not consume when scale is 1`() {
        val state = WebtoonZoomState(initialScale = 1f)
        val connection = state.scrollConnection

        val available = Offset(x = 25f, y = 100f)
        val consumed = connection.onPreScroll(available, NestedScrollSource.UserInput)

        assertEquals(Offset.Zero, consumed)
    }

    @Test
    fun `WebtoonZoomScrollConnection onPreScroll does not consume when source is not UserInput`() {
        val state = WebtoonZoomState(initialScale = 2.5f)
        val connection = state.scrollConnection

        val available = Offset(x = 0f, y = 100f)
        val consumed = connection.onPreScroll(available, NestedScrollSource.SideEffect)

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
}
