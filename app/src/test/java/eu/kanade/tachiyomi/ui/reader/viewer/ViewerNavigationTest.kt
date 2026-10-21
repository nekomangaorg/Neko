package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NormalizedRect
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.Region
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.TappingInvertMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerNavigationTest {

    private fun point(x: Float, y: Float): PointF =
        PointF(x, y).apply {
            this.x = x
            this.y = y
        }

    // --- NormalizedRect Tests ---

    @Test
    fun `given coordinate strictly inside NormalizedRect, when checking contains, then returns true`() {
        // Arrange
        val rect = NormalizedRect(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

        // Act & Assert
        assertTrue(rect.contains(0.5f, 0.5f))
    }

    @Test
    fun `given coordinate on inclusive top-left boundary, when checking contains, then returns true`() {
        // Arrange
        val rect = NormalizedRect(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

        // Act & Assert
        assertTrue(rect.contains(0.2f, 0.2f))
        assertTrue(rect.contains(0.2f, 0.5f))
        assertTrue(rect.contains(0.5f, 0.2f))
    }

    @Test
    fun `given coordinate on exclusive right or bottom boundary, when checking contains, then returns false`() {
        // Arrange
        val rect = NormalizedRect(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

        // Act & Assert
        assertFalse(rect.contains(0.8f, 0.5f))
        assertFalse(rect.contains(0.5f, 0.8f))
        assertFalse(rect.contains(0.8f, 0.8f))
    }

    @Test
    fun `given coordinate strictly outside NormalizedRect, when checking contains, then returns false`() {
        // Arrange
        val rect = NormalizedRect(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

        // Act & Assert
        assertFalse(rect.contains(0.1f, 0.5f)) // Left
        assertFalse(rect.contains(0.9f, 0.5f)) // Right
        assertFalse(rect.contains(0.5f, 0.1f)) // Above
        assertFalse(rect.contains(0.5f, 0.9f)) // Below
    }

    @Test
    fun `given invertMode NONE, when inverting NormalizedRect, then returns identical instance`() {
        // Arrange
        val rect = NormalizedRect(left = 0.1f, top = 0.2f, right = 0.3f, bottom = 0.4f)

        // Act
        val inverted = rect.invert(TappingInvertMode.NONE)

        // Assert
        assertSame(rect, inverted)
    }

    @Test
    fun `given invertMode HORIZONTAL, when inverting NormalizedRect, then horizontal coordinates are flipped`() {
        // Arrange
        val rect = NormalizedRect(left = 0.1f, top = 0.2f, right = 0.3f, bottom = 0.4f)

        // Act
        val inverted = rect.invert(TappingInvertMode.HORIZONTAL)

        // Assert
        assertEquals(0.7f, inverted.left, 0.001f) // 1 - 0.3
        assertEquals(0.2f, inverted.top, 0.001f)
        assertEquals(0.9f, inverted.right, 0.001f) // 1 - 0.1
        assertEquals(0.4f, inverted.bottom, 0.001f)
    }

    @Test
    fun `given invertMode VERTICAL, when inverting NormalizedRect, then vertical coordinates are flipped`() {
        // Arrange
        val rect = NormalizedRect(left = 0.1f, top = 0.2f, right = 0.3f, bottom = 0.4f)

        // Act
        val inverted = rect.invert(TappingInvertMode.VERTICAL)

        // Assert
        assertEquals(0.1f, inverted.left, 0.001f)
        assertEquals(0.6f, inverted.top, 0.001f) // 1 - 0.4
        assertEquals(0.3f, inverted.right, 0.001f)
        assertEquals(0.8f, inverted.bottom, 0.001f) // 1 - 0.2
    }

    @Test
    fun `given invertMode BOTH, when inverting NormalizedRect, then both dimensions are flipped`() {
        // Arrange
        val rect = NormalizedRect(left = 0.1f, top = 0.2f, right = 0.3f, bottom = 0.4f)

        // Act
        val inverted = rect.invert(TappingInvertMode.BOTH)

        // Assert
        assertEquals(0.7f, inverted.left, 0.001f)
        assertEquals(0.6f, inverted.top, 0.001f)
        assertEquals(0.9f, inverted.right, 0.001f)
        assertEquals(0.8f, inverted.bottom, 0.001f)
    }

    // --- Region Tests ---

    @Test
    fun `given Region with invertMode NONE, when inverting, then returns identical region instance`() {
        // Arrange
        val region = Region(NormalizedRect(0f, 0f, 0.5f, 1f), NavigationRegion.NEXT)

        // Act
        val inverted = region.invert(TappingInvertMode.NONE)

        // Assert
        assertSame(region, inverted)
    }

    @Test
    fun `given Region with invertMode HORIZONTAL, when inverting, then returns copy with inverted rect`() {
        // Arrange
        val region = Region(NormalizedRect(0f, 0f, 0.3f, 1f), NavigationRegion.NEXT)

        // Act
        val inverted = region.invert(TappingInvertMode.HORIZONTAL)

        // Assert
        assertEquals(0.7f, inverted.rect.left, 0.001f)
        assertEquals(1f, inverted.rect.right, 0.001f)
        assertEquals(NavigationRegion.NEXT, inverted.type)
    }

    // --- NavigationRegion.directionalRegion Tests ---

    @Test
    fun `given directional NavigationRegion LEFT, resolves to PREV in LTR and NEXT in RTL`() {
        assertEquals(NavigationRegion.PREV, NavigationRegion.LEFT.directionalRegion(LTR = true))
        assertEquals(NavigationRegion.NEXT, NavigationRegion.LEFT.directionalRegion(LTR = false))
    }

    @Test
    fun `given directional NavigationRegion RIGHT, resolves to NEXT in LTR and PREV in RTL`() {
        assertEquals(NavigationRegion.NEXT, NavigationRegion.RIGHT.directionalRegion(LTR = true))
        assertEquals(NavigationRegion.PREV, NavigationRegion.RIGHT.directionalRegion(LTR = false))
    }

    @Test
    fun `given non-directional regions, directionalRegion returns identical region instance in both directions`() {
        assertSame(NavigationRegion.MENU, NavigationRegion.MENU.directionalRegion(LTR = true))
        assertSame(NavigationRegion.MENU, NavigationRegion.MENU.directionalRegion(LTR = false))

        assertSame(NavigationRegion.NEXT, NavigationRegion.NEXT.directionalRegion(LTR = true))
        assertSame(NavigationRegion.NEXT, NavigationRegion.NEXT.directionalRegion(LTR = false))

        assertSame(NavigationRegion.PREV, NavigationRegion.PREV.directionalRegion(LTR = true))
        assertSame(NavigationRegion.PREV, NavigationRegion.PREV.directionalRegion(LTR = false))
    }

    // --- ViewerNavigation.getAction Tests ---

    @Test
    fun `given tap in top 5 percent constant menu region, when resolving action, then returns MENU`() {
        // Arrange
        val nav =
            object : ViewerNavigation() {
                override var regions: List<Region> =
                    listOf(Region(NormalizedRect(0f, 0.2f, 1f, 1f), NavigationRegion.NEXT))
            }

        // Act
        val action = nav.getAction(point(0.5f, 0.02f))

        // Assert
        assertEquals(NavigationRegion.MENU, action)
    }

    @Test
    fun `given tap within defined navigation region, when resolving action, then returns region type`() {
        // Arrange
        val nav =
            object : ViewerNavigation() {
                override var regions: List<Region> =
                    listOf(
                        Region(NormalizedRect(0f, 0.1f, 0.5f, 1f), NavigationRegion.PREV),
                        Region(NormalizedRect(0.5f, 0.1f, 1f, 1f), NavigationRegion.NEXT),
                    )
            }

        // Act & Assert
        assertEquals(NavigationRegion.PREV, nav.getAction(point(0.25f, 0.5f)))
        assertEquals(NavigationRegion.NEXT, nav.getAction(point(0.75f, 0.5f)))
    }

    @Test
    fun `given tap with HORIZONTAL invertMode set, when resolving action, then maps to horizontally flipped region`() {
        // Arrange
        val nav =
            object : ViewerNavigation() {
                override var regions: List<Region> =
                    listOf(Region(NormalizedRect(0f, 0.1f, 0.5f, 1f), NavigationRegion.PREV))
            }
        nav.invertMode = TappingInvertMode.HORIZONTAL

        // Act & Assert
        // Inverted rect is now [0.5, 0.1, 1.0, 1.0]
        assertEquals(NavigationRegion.PREV, nav.getAction(point(0.75f, 0.5f)))
        assertEquals(NavigationRegion.MENU, nav.getAction(point(0.25f, 0.5f))) // Now unmapped
    }

    @Test
    fun `given tap in unmapped region outside constant menu, when resolving action, then defaults to MENU`() {
        // Arrange
        val nav =
            object : ViewerNavigation() {
                override var regions: List<Region> =
                    listOf(Region(NormalizedRect(0f, 0.5f, 0.5f, 1f), NavigationRegion.NEXT))
            }

        // Act
        val action = nav.getAction(point(0.8f, 0.2f))

        // Assert
        assertEquals(NavigationRegion.MENU, action)
    }
}
