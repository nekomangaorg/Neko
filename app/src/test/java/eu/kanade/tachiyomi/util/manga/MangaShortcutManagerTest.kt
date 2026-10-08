package eu.kanade.tachiyomi.util.manga

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class MangaShortcutManagerTest {

    private lateinit var shortcutManager: MangaShortcutManager

    @Before
    fun setup() {
        shortcutManager =
            MangaShortcutManager(
                preferences = mockk(relaxed = true),
                coverCache = mockk(relaxed = true),
                sourceManager = mockk(relaxed = true),
            )
    }

    @Test
    fun `toSquare returns input directly when bitmap is recycled`() {
        val mockBitmap = mockk<Bitmap> { every { isRecycled } returns true }

        with(shortcutManager) {
            val result = mockBitmap.toSquare()
            assertSame(mockBitmap, result)
        }
    }

    @Test
    fun `toSquare returns input directly when dimensions are invalid`() {
        val mockZeroWidth =
            mockk<Bitmap> {
                every { isRecycled } returns false
                every { width } returns 0
                every { height } returns 100
            }
        val mockNegativeHeight =
            mockk<Bitmap> {
                every { isRecycled } returns false
                every { width } returns 100
                every { height } returns -5
            }

        with(shortcutManager) {
            assertSame(mockZeroWidth, mockZeroWidth.toSquare())
            assertSame(mockNegativeHeight, mockNegativeHeight.toSquare())
        }
    }

    @Test
    fun `toSquare catches exception and returns input directly without crashing`() {
        val mockBitmap =
            mockk<Bitmap> {
                every { isRecycled } returns false
                every { width } returns 200
                every { height } returns 300
                every { config } returns null
                every { colorSpace } returns null
            }

        with(shortcutManager) {
            val result = mockBitmap.toSquare()
            assertSame(mockBitmap, result)
        }
    }
}
