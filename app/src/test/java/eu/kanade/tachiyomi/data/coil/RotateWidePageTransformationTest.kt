package eu.kanade.tachiyomi.data.coil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotateWidePageTransformationTest {

    @Test
    fun `shouldRotate returns true when rotateWide is true and width is greater than height`() {
        val result =
            RotateWidePageTransformation.shouldRotate(
                width = 1920,
                height = 1080,
                rotateWide = true,
            )
        assertTrue(result)
    }

    @Test
    fun `shouldRotate returns false when rotateWide is false`() {
        val result =
            RotateWidePageTransformation.shouldRotate(
                width = 1920,
                height = 1080,
                rotateWide = false,
            )
        assertFalse(result)
    }

    @Test
    fun `shouldRotate returns false when image is tall or portrait`() {
        val result =
            RotateWidePageTransformation.shouldRotate(
                width = 1080,
                height = 1920,
                rotateWide = true,
            )
        assertFalse(result)
    }

    @Test
    fun `shouldRotate returns false when image is square`() {
        val result =
            RotateWidePageTransformation.shouldRotate(
                width = 1000,
                height = 1000,
                rotateWide = true,
            )
        assertFalse(result)
    }

    @Test
    fun `shouldRotate boundary returns true when width is exactly one pixel greater than height`() {
        assertTrue(RotateWidePageTransformation.shouldRotate(1001, 1000, true))
    }

    @Test
    fun `shouldRotate boundary returns false when width is exactly one pixel less than height`() {
        assertFalse(RotateWidePageTransformation.shouldRotate(1000, 1001, true))
    }

    @Test
    fun `shouldRotate handles 1x1 image as not wide`() {
        assertFalse(RotateWidePageTransformation.shouldRotate(1, 1, true))
    }

    @Test
    fun `shouldRotate returns true for ultra-high-resolution double spreads`() {
        assertTrue(RotateWidePageTransformation.shouldRotate(8000, 4500, true))
    }

    @Test
    fun `shouldRotate returns false when dimensions are zero or negative`() {
        assertFalse(RotateWidePageTransformation.shouldRotate(0, 100, true))
        assertFalse(RotateWidePageTransformation.shouldRotate(100, 0, true))
        assertFalse(RotateWidePageTransformation.shouldRotate(-100, 50, true))
        assertFalse(RotateWidePageTransformation.shouldRotate(100, -50, true))
        assertFalse(RotateWidePageTransformation.shouldRotate(-100, -100, true))
    }

    @Test
    fun `rotationDegrees returns 90f when reverse is false`() {
        assertEquals(90f, RotateWidePageTransformation.rotationDegrees(reverse = false))
    }

    @Test
    fun `rotationDegrees returns -90f when reverse is true`() {
        assertEquals(-90f, RotateWidePageTransformation.rotationDegrees(reverse = true))
    }

    @Test
    fun `default constructor sets rotateWide to true and reverse to false`() {
        val transformation = RotateWidePageTransformation()
        assertTrue(transformation.rotateWide)
        assertFalse(transformation.reverse)
    }

    @Test
    fun `RotateWidePageTransformation cacheKey reflects configuration parameters`() {
        val t1 = RotateWidePageTransformation(rotateWide = true, reverse = false)
        val t2 = RotateWidePageTransformation(rotateWide = true, reverse = true)
        val t3 = RotateWidePageTransformation(rotateWide = false, reverse = false)
        val t4 = RotateWidePageTransformation(rotateWide = false, reverse = true)

        assertNotEquals(t1.cacheKey, t2.cacheKey)
        assertNotEquals(t1.cacheKey, t3.cacheKey)
        assertNotEquals(t1.cacheKey, t4.cacheKey)
        assertNotEquals(t2.cacheKey, t3.cacheKey)
        assertNotEquals(t2.cacheKey, t4.cacheKey)
        assertNotEquals(t3.cacheKey, t4.cacheKey)
    }

    @Test
    fun `RotateWidePageTransformation equals and hashCode contract is satisfied`() {
        val t1 = RotateWidePageTransformation(rotateWide = true, reverse = false)
        val t2 = RotateWidePageTransformation(rotateWide = true, reverse = false)
        val t3 = RotateWidePageTransformation(rotateWide = true, reverse = true)
        val t4 = RotateWidePageTransformation(rotateWide = false, reverse = false)

        // Reflexive
        assertEquals(t1, t1)
        // Symmetric
        assertEquals(t1, t2)
        assertEquals(t2, t1)
        assertEquals(t1.hashCode(), t2.hashCode())

        // Unequal instances
        assertNotEquals(t1, t3)
        assertNotEquals(t1, t4)
        assertNotEquals(t3, t4)

        // Unequal to null and different type
        assertNotEquals(t1, null)
        assertNotEquals(t1, "not-a-transformation")
    }

    @Test
    fun `RotateWidePageTransformation toString contains class name and parameter values`() {
        val t = RotateWidePageTransformation(rotateWide = true, reverse = false)
        val stringRepresentation = t.toString()
        assertTrue(stringRepresentation.contains("RotateWidePageTransformation"))
        assertTrue(stringRepresentation.contains("rotateWide=true"))
        assertTrue(stringRepresentation.contains("reverse=false"))
    }
}
