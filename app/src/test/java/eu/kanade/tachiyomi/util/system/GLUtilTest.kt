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
    fun `SAFE_CANVAS_BITMAP_DIMENSION is 4096`() {
        assertEquals(4096, GLUtil.SAFE_CANVAS_BITMAP_DIMENSION)
    }

    @Test
    fun `maxCanvasTextureSize does not exceed SAFE_CANVAS_BITMAP_DIMENSION and is at least minimum default`() {
        assertTrue(GLUtil.maxCanvasTextureSize <= GLUtil.SAFE_CANVAS_BITMAP_DIMENSION)
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
