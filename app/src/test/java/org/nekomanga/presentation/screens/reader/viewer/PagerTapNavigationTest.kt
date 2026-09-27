package org.nekomanga.presentation.screens.reader.viewer

import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagerTapNavigationTest {

    @Test
    fun `dispatchNavigation with MENU action toggles menu and does not navigate`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.MENU,
            isRtl = false,
            menuVisible = false,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertTrue(menuToggled)
        assertEquals(null, navigated)
    }

    @Test
    fun `dispatchNavigation with NEXT action navigates forward`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.NEXT,
            isRtl = false,
            menuVisible = false,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertFalse(menuToggled)
        assertEquals(true, navigated)
    }

    @Test
    fun `dispatchNavigation with PREV action navigates backward`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.PREV,
            isRtl = false,
            menuVisible = false,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertFalse(menuToggled)
        assertEquals(false, navigated)
    }

    @Test
    fun `dispatchNavigation with RIGHT action respects LTR and RTL directions`() {
        var navigatedLtr: Boolean? = null
        dispatchNavigation(
            action = NavigationRegion.RIGHT,
            isRtl = false,
            menuVisible = false,
            onToggleMenu = {},
            onNavigateAdjacent = { forward -> navigatedLtr = forward },
        )
        // In LTR, RIGHT navigates forward
        assertEquals(true, navigatedLtr)

        var navigatedRtl: Boolean? = null
        dispatchNavigation(
            action = NavigationRegion.RIGHT,
            isRtl = true,
            menuVisible = false,
            onToggleMenu = {},
            onNavigateAdjacent = { forward -> navigatedRtl = forward },
        )
        // In RTL, RIGHT navigates backward
        assertEquals(false, navigatedRtl)
    }

    @Test
    fun `dispatchNavigation with LEFT action respects LTR and RTL directions`() {
        var navigatedLtr: Boolean? = null
        dispatchNavigation(
            action = NavigationRegion.LEFT,
            isRtl = false,
            menuVisible = false,
            onToggleMenu = {},
            onNavigateAdjacent = { forward -> navigatedLtr = forward },
        )
        // In LTR, LEFT navigates backward
        assertEquals(false, navigatedLtr)

        var navigatedRtl: Boolean? = null
        dispatchNavigation(
            action = NavigationRegion.LEFT,
            isRtl = true,
            menuVisible = false,
            onToggleMenu = {},
            onNavigateAdjacent = { forward -> navigatedRtl = forward },
        )
        // In RTL, LEFT navigates forward
        assertEquals(true, navigatedRtl)
    }

    @Test
    fun `dispatchNavigation with menuVisible dismisses menu before navigating adjacent`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.NEXT,
            isRtl = false,
            menuVisible = true,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertTrue("Menu should be dismissed when navigating with visible menu", menuToggled)
        assertEquals(true, navigated)
    }
}
