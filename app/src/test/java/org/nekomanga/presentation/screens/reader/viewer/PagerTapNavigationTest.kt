package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.geometry.Offset
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

    @Test
    fun `dispatchNavigation with PREV action and menuVisible dismisses menu before navigating backward`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.PREV,
            isRtl = false,
            menuVisible = true,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertTrue(
            "Menu should be dismissed when navigating backward with visible menu",
            menuToggled,
        )
        assertEquals(false, navigated)
    }

    @Test
    fun `dispatchNavigation with MENU action and menuVisible toggles menu without navigating`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.MENU,
            isRtl = false,
            menuVisible = true,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertTrue(menuToggled)
        assertEquals(null, navigated)
    }

    @Test
    fun `dispatchNavigation with RIGHT in RTL and menuVisible dismisses menu and navigates backward`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.RIGHT,
            isRtl = true,
            menuVisible = true,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertTrue(menuToggled)
        assertEquals(false, navigated)
    }

    @Test
    fun `dispatchNavigation with LEFT in RTL and menuVisible dismisses menu and navigates forward`() {
        var menuToggled = false
        var navigated: Boolean? = null

        dispatchNavigation(
            action = NavigationRegion.LEFT,
            isRtl = true,
            menuVisible = true,
            onToggleMenu = { menuToggled = true },
            onNavigateAdjacent = { forward -> navigated = forward },
        )

        assertTrue(menuToggled)
        assertEquals(true, navigated)
    }

    @Test
    fun `evaluatePointerMovement returns VALID_TAP_UP when finger is lifted within touch slop`() {
        val down = Offset(100f, 100f)
        val upWithinSlop = Offset(104f, 103f) // distance = 5px, distanceSquared = 25px
        val touchSlopSquared = 64f // slop = 8px

        val result =
            evaluatePointerMovement(
                downPos = down,
                currentPos = upWithinSlop,
                isPressed = false,
                touchSlopSquared = touchSlopSquared,
            )

        assertEquals(PointerSlopResult.VALID_TAP_UP, result)
    }

    @Test
    fun `evaluatePointerMovement returns VALID_TAP_UP on zero movement finger lift`() {
        val down = Offset(100f, 100f)
        val touchSlopSquared = 64f

        val result =
            evaluatePointerMovement(
                downPos = down,
                currentPos = down,
                isPressed = false,
                touchSlopSquared = touchSlopSquared,
            )

        assertEquals(PointerSlopResult.VALID_TAP_UP, result)
    }

    @Test
    fun `evaluatePointerMovement returns MOVEMENT_PAST_SLOP when finger lift exceeds touch slop`() {
        val down = Offset(100f, 100f)
        val upPastSlop = Offset(110f, 100f) // distance = 10px, distanceSquared = 100px
        val touchSlopSquared = 64f // slop = 8px

        val result =
            evaluatePointerMovement(
                downPos = down,
                currentPos = upPastSlop,
                isPressed = false,
                touchSlopSquared = touchSlopSquared,
            )

        assertEquals(PointerSlopResult.MOVEMENT_PAST_SLOP, result)
    }

    @Test
    fun `evaluatePointerMovement returns WITHIN_SLOP_PRESSED when finger is held down and within touch slop`() {
        val down = Offset(100f, 100f)
        val moveWithinSlop = Offset(103f, 100f) // distance = 3px, distanceSquared = 9px
        val touchSlopSquared = 64f

        val result =
            evaluatePointerMovement(
                downPos = down,
                currentPos = moveWithinSlop,
                isPressed = true,
                touchSlopSquared = touchSlopSquared,
            )

        assertEquals(PointerSlopResult.WITHIN_SLOP_PRESSED, result)
    }

    @Test
    fun `evaluatePointerMovement returns MOVEMENT_PAST_SLOP when finger is held down and exceeds touch slop`() {
        val down = Offset(100f, 100f)
        val movePastSlop = Offset(115f, 100f) // distance = 15px, distanceSquared = 225px
        val touchSlopSquared = 64f

        val result =
            evaluatePointerMovement(
                downPos = down,
                currentPos = movePastSlop,
                isPressed = true,
                touchSlopSquared = touchSlopSquared,
            )

        assertEquals(PointerSlopResult.MOVEMENT_PAST_SLOP, result)
    }

    @Test
    fun `isDoubleTap returns true when second tap is within timeout and slop distance`() {
        val result =
            isDoubleTap(
                upTime = 500L,
                lastTapTime = 300L, // 200ms < 300ms timeout
                doubleTapTimeoutMs = 300L,
                upPos = Offset(102f, 100f),
                lastTapOffset = Offset(100f, 100f), // distance = 2px, distanceSquared = 4px
                doubleTapSlopSquared = 100f,
                hasDoubleTapAnimation = true,
            )

        assertTrue(result)
    }

    @Test
    fun `isDoubleTap returns false when second tap exceeds timeout`() {
        val result =
            isDoubleTap(
                upTime = 700L,
                lastTapTime = 300L, // 400ms > 300ms timeout
                doubleTapTimeoutMs = 300L,
                upPos = Offset(100f, 100f),
                lastTapOffset = Offset(100f, 100f),
                doubleTapSlopSquared = 100f,
                hasDoubleTapAnimation = true,
            )

        assertFalse(result)
    }

    @Test
    fun `isDoubleTap returns false when second tap exceeds slop distance`() {
        val result =
            isDoubleTap(
                upTime = 400L,
                lastTapTime = 300L,
                doubleTapTimeoutMs = 300L,
                upPos = Offset(150f, 100f), // distance = 50px, distanceSquared = 2500px
                lastTapOffset = Offset(100f, 100f),
                doubleTapSlopSquared = 100f,
                hasDoubleTapAnimation = true,
            )

        assertFalse(result)
    }

    @Test
    fun `isDoubleTap returns false when double tap animation is disabled`() {
        val result =
            isDoubleTap(
                upTime = 400L,
                lastTapTime = 300L,
                doubleTapTimeoutMs = 300L,
                upPos = Offset(100f, 100f),
                lastTapOffset = Offset(100f, 100f),
                doubleTapSlopSquared = 100f,
                hasDoubleTapAnimation = false,
            )

        assertFalse(result)
    }

    @Test
    fun `resolveTapAction resolves navigation regions to CANCEL_NAV_FOR_ZOOM during double tap with animation enabled`() {
        val actions =
            listOf(
                NavigationRegion.NEXT,
                NavigationRegion.PREV,
                NavigationRegion.LEFT,
                NavigationRegion.RIGHT,
            )

        for (action in actions) {
            val result =
                resolveTapAction(
                    action = action,
                    menuVisible = false,
                    isDoubleTap = true,
                    hasDoubleTapAnimation = true,
                )
            assertEquals(TapActionResolution.CANCEL_NAV_FOR_ZOOM, result)
        }
    }

    @Test
    fun `resolveTapAction resolves navigation regions to SCHEDULE_NAV_DELAYED for single tap with animation enabled`() {
        val actions =
            listOf(
                NavigationRegion.NEXT,
                NavigationRegion.PREV,
                NavigationRegion.LEFT,
                NavigationRegion.RIGHT,
            )

        for (action in actions) {
            val result =
                resolveTapAction(
                    action = action,
                    menuVisible = false,
                    isDoubleTap = false,
                    hasDoubleTapAnimation = true,
                )
            assertEquals(TapActionResolution.SCHEDULE_NAV_DELAYED, result)
        }
    }

    @Test
    fun `resolveTapAction resolves navigation regions to NAVIGATE_IMMEDIATELY when animation is disabled`() {
        val actions =
            listOf(
                NavigationRegion.NEXT,
                NavigationRegion.PREV,
                NavigationRegion.LEFT,
                NavigationRegion.RIGHT,
            )

        for (action in actions) {
            val result =
                resolveTapAction(
                    action = action,
                    menuVisible = false,
                    isDoubleTap = false,
                    hasDoubleTapAnimation = false,
                )
            assertEquals(TapActionResolution.NAVIGATE_IMMEDIATELY, result)
        }
    }

    @Test
    fun `resolveTapAction resolves navigation regions to NAVIGATE_IMMEDIATELY when menu is currently visible`() {
        val actions =
            listOf(
                NavigationRegion.NEXT,
                NavigationRegion.PREV,
                NavigationRegion.LEFT,
                NavigationRegion.RIGHT,
            )

        for (action in actions) {
            val result =
                resolveTapAction(
                    action = action,
                    menuVisible = true,
                    isDoubleTap = false,
                    hasDoubleTapAnimation = true,
                )
            assertEquals(TapActionResolution.NAVIGATE_IMMEDIATELY, result)
        }
    }

    @Test
    fun `evaluateDownPointer returns CANCEL_PENDING_FOR_DOUBLE_TAP when second tap arrives within timeout and slop`() {
        val result =
            evaluateDownPointer(
                hasActivePendingJob = true,
                downPos = Offset(102f, 100f),
                lastTapOffset = Offset(100f, 100f), // distance = 2px, distanceSquared = 4px
                downTime = 450L,
                lastTapTime = 300L, // 150ms < 300ms timeout
                doubleTapTimeoutMs = 300L,
                doubleTapSlopSquared = 100f,
            )

        assertEquals(DownPointerAction.CANCEL_PENDING_FOR_DOUBLE_TAP, result)
    }

    @Test
    fun `evaluateDownPointer returns FLUSH_PENDING_AND_START_NEW when second tap exceeds slop`() {
        val result =
            evaluateDownPointer(
                hasActivePendingJob = true,
                downPos = Offset(150f, 100f), // distance = 50px, distanceSquared = 2500px
                lastTapOffset = Offset(100f, 100f),
                downTime = 450L,
                lastTapTime = 300L,
                doubleTapTimeoutMs = 300L,
                doubleTapSlopSquared = 100f,
            )

        assertEquals(DownPointerAction.FLUSH_PENDING_AND_START_NEW, result)
    }

    @Test
    fun `evaluateDownPointer returns FLUSH_PENDING_AND_START_NEW when second tap exceeds timeout`() {
        val result =
            evaluateDownPointer(
                hasActivePendingJob = true,
                downPos = Offset(102f, 100f),
                lastTapOffset = Offset(100f, 100f),
                downTime = 700L, // 400ms > 300ms timeout
                lastTapTime = 300L,
                doubleTapTimeoutMs = 300L,
                doubleTapSlopSquared = 100f,
            )

        assertEquals(DownPointerAction.FLUSH_PENDING_AND_START_NEW, result)
    }

    @Test
    fun `evaluateDownPointer returns NO_OP when no active pending job`() {
        val result =
            evaluateDownPointer(
                hasActivePendingJob = false,
                downPos = Offset(100f, 100f),
                lastTapOffset = Offset(100f, 100f),
                downTime = 350L,
                lastTapTime = 300L,
                doubleTapTimeoutMs = 300L,
                doubleTapSlopSquared = 100f,
            )

        assertEquals(DownPointerAction.NO_OP, result)
    }

    @Test
    fun `resolveTapAction resolves MENU to TOGGLE_MENU_IMMEDIATE when menu is currently visible`() {
        val result =
            resolveTapAction(
                action = NavigationRegion.MENU,
                menuVisible = true,
                isDoubleTap = false,
                hasDoubleTapAnimation = true,
            )

        assertEquals(TapActionResolution.TOGGLE_MENU_IMMEDIATE, result)
    }

    @Test
    fun `resolveTapAction resolves MENU to CANCEL_MENU_FOR_ZOOM when menu is hidden and isDoubleTap is true`() {
        val result =
            resolveTapAction(
                action = NavigationRegion.MENU,
                menuVisible = false,
                isDoubleTap = true,
                hasDoubleTapAnimation = true,
            )

        assertEquals(TapActionResolution.CANCEL_MENU_FOR_ZOOM, result)
    }

    @Test
    fun `resolveTapAction resolves MENU to SCHEDULE_MENU_DELAYED when menu is hidden, single tap, and animation enabled`() {
        val result =
            resolveTapAction(
                action = NavigationRegion.MENU,
                menuVisible = false,
                isDoubleTap = false,
                hasDoubleTapAnimation = true,
            )

        assertEquals(TapActionResolution.SCHEDULE_MENU_DELAYED, result)
    }

    @Test
    fun `resolveTapAction resolves MENU to TOGGLE_MENU_IMMEDIATE when menu is hidden, single tap, and animation disabled`() {
        val result =
            resolveTapAction(
                action = NavigationRegion.MENU,
                menuVisible = false,
                isDoubleTap = false,
                hasDoubleTapAnimation = false,
            )

        assertEquals(TapActionResolution.TOGGLE_MENU_IMMEDIATE, result)
    }
}
