package eu.kanade.tachiyomi.util.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GLUtilTest {

    @Test
    fun `MAX_CANVAS_BITMAP_BYTES is exactly 100 MB`() {
        assertEquals(104857600L, GLUtil.MAX_CANVAS_BITMAP_BYTES)
    }

    @Test
    fun `maxCanvasTextureSize does not exceed 4096 and is at least minimum default`() {
        assertTrue(GLUtil.maxCanvasTextureSize <= 4096)
        assertTrue(GLUtil.maxCanvasTextureSize >= 2048)
    }

    @Test
    fun `square ARGB_8888 bitmap at maxCanvasTextureSize does not exceed MAX_CANVAS_BITMAP_BYTES`() {
        val maxDim = GLUtil.maxCanvasTextureSize.toLong()
        val maxBytes = maxDim * maxDim * 4L
        assertTrue(
            "Max canvas bitmap bytes ($maxBytes) must not exceed MAX_CANVAS_BITMAP_BYTES (${GLUtil.MAX_CANVAS_BITMAP_BYTES})",
            maxBytes <= GLUtil.MAX_CANVAS_BITMAP_BYTES,
        )
    }
}
