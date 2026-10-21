package org.nekomanga.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.R

class UiTextTest {

    // --- UiText.String Tests ---

    @Test
    fun `given identical String instances, when compared, then equals and hashCode match`() {
        // Arrange
        val text1 = UiText.String("Hello World")
        val text2 = UiText.String("Hello World")

        // Act & Assert
        assertEquals(text1, text2)
        assertEquals(text1.hashCode(), text2.hashCode())
    }

    @Test
    fun `given two different String instances, when compared, then equals is false`() {
        // Arrange
        val text1 = UiText.String("Hello")
        val text2 = UiText.String("World")

        // Act & Assert
        assertNotEquals(text1, text2)
    }

    // --- UiText.StringResource Tests ---

    @Test
    fun `given identical StringResource without format args, when compared, then equals and hashCode match`() {
        // Arrange
        val res1 = UiText.StringResource(R.string.next)
        val res2 = UiText.StringResource(R.string.next)

        // Act & Assert
        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
    }

    @Test
    fun `given identical StringResource with matching format args, when compared, then equals and hashCode match`() {
        // Arrange
        val res1 = UiText.StringResource(R.string.page_, 1)
        val res2 = UiText.StringResource(R.string.page_, 1)

        // Act & Assert
        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
    }

    @Test
    fun `given StringResource instances with different resource IDs, when compared, then equals is false`() {
        // Arrange
        val res1 = UiText.StringResource(R.string.next)
        val res2 = UiText.StringResource(R.string.previous)

        // Act & Assert
        assertNotEquals(res1, res2)
    }

    @Test
    fun `given StringResource instances with different format args, when compared, then equals is false`() {
        // Arrange
        val res1 = UiText.StringResource(R.string.page_, 1)
        val res2 = UiText.StringResource(R.string.page_, 2)

        // Act & Assert
        assertNotEquals(res1, res2)
    }

    @Test
    fun `given StringResource compared to itself, then equals is true`() {
        // Arrange
        val res = UiText.StringResource(R.string.next, "arg")

        // Act & Assert
        assertTrue(res == res)
    }

    @Test
    fun `given StringResource compared to null or other type, then equals is false`() {
        // Arrange
        val res = UiText.StringResource(R.string.next)

        // Act & Assert
        assertFalse(res.equals(null))
        assertFalse(res.equals("not a UiText"))
    }

    // --- UiText.PluralsResource Tests ---

    @Test
    fun `given identical PluralsResource with matching count and format args, when compared, then equals and hashCode match`() {
        // Arrange
        val plurals1 = UiText.PluralsResource(R.plurals.after_minutes, 5, 5)
        val plurals2 = UiText.PluralsResource(R.plurals.after_minutes, 5, 5)

        // Act & Assert
        assertEquals(plurals1, plurals2)
        assertEquals(plurals1.hashCode(), plurals2.hashCode())
    }

    @Test
    fun `given PluralsResource instances with different counts, when compared, then equals is false`() {
        // Arrange
        val plurals1 = UiText.PluralsResource(R.plurals.after_minutes, 5, 5)
        val plurals2 = UiText.PluralsResource(R.plurals.after_minutes, 10, 5)

        // Act & Assert
        assertNotEquals(plurals1, plurals2)
    }

    @Test
    fun `given PluralsResource instances with different resource IDs, when compared, then equals is false`() {
        // Arrange
        val plurals1 = UiText.PluralsResource(R.plurals.after_minutes, 5)
        val plurals2 = UiText.PluralsResource(R.plurals.missing_chapters_warning, 5)

        // Act & Assert
        assertNotEquals(plurals1, plurals2)
    }

    @Test
    fun `given PluralsResource instances with different format args, when compared, then equals is false`() {
        // Arrange
        val plurals1 = UiText.PluralsResource(R.plurals.after_minutes, 5, "a")
        val plurals2 = UiText.PluralsResource(R.plurals.after_minutes, 5, "b")

        // Act & Assert
        assertNotEquals(plurals1, plurals2)
    }

    @Test
    fun `given PluralsResource compared to itself, then equals is true`() {
        // Arrange
        val plurals = UiText.PluralsResource(R.plurals.after_minutes, 5)

        // Act & Assert
        assertTrue(plurals == plurals)
    }

    @Test
    fun `given PluralsResource compared to null or other type, then equals is false`() {
        // Arrange
        val plurals = UiText.PluralsResource(R.plurals.after_minutes, 5)

        // Act & Assert
        assertFalse(plurals.equals(null))
        assertFalse(plurals.equals("not a UiText"))
    }
}
