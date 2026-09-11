package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.KeyEvent
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderNavigationTest {

    // --- Key Navigation Tests ---

    @Test
    fun `given KEYCODE_N in LTR mode, when resolving direction, then returns true (next chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(
                keyCode = KeyEvent.KEYCODE_N,
                isRtl = false,
            )

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `given KEYCODE_N in RTL mode, when resolving direction, then returns true (next chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(keyCode = KeyEvent.KEYCODE_N, isRtl = true)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `given KEYCODE_P in LTR mode, when resolving direction, then returns false (previous chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(
                keyCode = KeyEvent.KEYCODE_P,
                isRtl = false,
            )

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `given KEYCODE_P in RTL mode, when resolving direction, then returns false (previous chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(keyCode = KeyEvent.KEYCODE_P, isRtl = true)

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `given KEYCODE_L (physical left) in LTR mode, when resolving direction, then returns false (previous chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(
                keyCode = KeyEvent.KEYCODE_L,
                isRtl = false,
            )

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `given KEYCODE_L (physical left) in RTL mode, when resolving direction, then returns true (next chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(keyCode = KeyEvent.KEYCODE_L, isRtl = true)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `given KEYCODE_R (physical right) in LTR mode, when resolving direction, then returns true (next chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(
                keyCode = KeyEvent.KEYCODE_R,
                isRtl = false,
            )

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `given KEYCODE_R (physical right) in RTL mode, when resolving direction, then returns false (previous chapter)`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(keyCode = KeyEvent.KEYCODE_R, isRtl = true)

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `given unhandled keycode, when resolving direction, then returns null`() {
        // Arrange & Act
        val result =
            ReaderKeyNavigation.resolveAdjacentDirection(
                keyCode = KeyEvent.KEYCODE_A,
                isRtl = false,
            )

        // Assert
        assertNull(result)
    }

    // --- ChapterNavTarget Resolution Tests ---

    @Test
    fun `given ChapterNavTarget End for brand new unread chapter, when resolving target page, then returns last page index`() {
        // Arrange
        val target = ChapterNavTarget.End

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 25,
                isRead = false,
                lastPageRead = 0,
                pagesLeft = 25,
            )

        // Assert
        assertEquals(24, page)
    }

    @Test
    fun `given ChapterNavTarget End for fully read chapter, when resolving target page, then returns last page index`() {
        // Arrange
        val target = ChapterNavTarget.End

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 20,
                isRead = true,
                lastPageRead = 19,
                pagesLeft = 0,
            )

        // Assert
        assertEquals(19, page)
    }

    @Test
    fun `given ChapterNavTarget Start, when resolving target page, then returns 0`() {
        // Arrange
        val target = ChapterNavTarget.Start

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 30,
                isRead = false,
                lastPageRead = 10,
                pagesLeft = 20,
            )

        // Assert
        assertEquals(0, page)
    }

    @Test
    fun `given ChapterNavTarget Resume for brand new chapter, when resolving target page, then returns 0`() {
        // Arrange
        val target = ChapterNavTarget.Resume

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 30,
                isRead = false,
                lastPageRead = 0,
                pagesLeft = 30,
            )

        // Assert
        assertEquals(0, page)
    }

    @Test
    fun `given ChapterNavTarget Resume for partially read chapter, when resolving target page, then returns lastPageRead`() {
        // Arrange
        val target = ChapterNavTarget.Resume

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 30,
                isRead = false,
                lastPageRead = 15,
                pagesLeft = 15,
            )

        // Assert
        assertEquals(15, page)
    }

    @Test
    fun `given ChapterNavTarget Resume for chapter with 1 page left but not read, when resolving target page, then preserves lastPageRead`() {
        // Arrange
        val target = ChapterNavTarget.Resume

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 30,
                isRead = false,
                lastPageRead = 29,
                pagesLeft = 1,
            )

        // Assert
        assertEquals(29, page)
    }

    @Test
    fun `given ChapterNavTarget Resume for already read chapter, when resolving target page, then restarts at 0`() {
        // Arrange
        val target = ChapterNavTarget.Resume

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 30,
                isRead = true,
                lastPageRead = 29,
                pagesLeft = 0,
            )

        // Assert
        assertEquals(0, page)
    }

    @Test
    fun `given ChapterNavTarget End for chapter with 0 pages, when resolving target page, then returns 0`() {
        // Arrange
        val target = ChapterNavTarget.End

        // Act
        val page =
            target.resolveRequestedPage(
                pageCount = 0,
                isRead = false,
                lastPageRead = 0,
                pagesLeft = 0,
            )

        // Assert
        assertEquals(0, page)
    }
}
