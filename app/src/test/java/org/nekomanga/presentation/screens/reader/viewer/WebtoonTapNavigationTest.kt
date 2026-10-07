package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.input.pointer.PointerId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebtoonTapNavigationTest {

    @Test
    fun `shouldDropPendingTap keeps the waiting tap for a plain tap`() {
        assertFalse(shouldDropPendingTap(isMovementPastSlop = false, isClaimed = false))
    }

    @Test
    fun `shouldDropPendingTap drops the waiting tap once the finger moves past slop`() {
        assertTrue(shouldDropPendingTap(isMovementPastSlop = true, isClaimed = false))
    }

    @Test
    fun `shouldDropPendingTap drops the waiting tap for a tap on a claimed control such as Retry`() {
        assertTrue(shouldDropPendingTap(isMovementPastSlop = false, isClaimed = true))
    }

    @Test
    fun `canFirePendingTap fires when no finger is down`() {
        assertTrue(canFirePendingTap(heldPointer = null, tapClaim = TapNavigationClaim()))
    }

    @Test
    fun `canFirePendingTap fires when the held finger is on the page`() {
        val claim = TapNavigationClaim().apply { claim(PointerId(1)) }

        assertTrue(canFirePendingTap(heldPointer = PointerId(2), tapClaim = claim))
    }

    @Test
    fun `canFirePendingTap holds back while the finger rests on a claimed control such as Retry`() {
        val claim = TapNavigationClaim().apply { claim(PointerId(1)) }

        assertFalse(canFirePendingTap(heldPointer = PointerId(1), tapClaim = claim))
    }

    @Test
    fun `canFirePendingTap fires when the viewer has no claim`() {
        assertTrue(canFirePendingTap(heldPointer = PointerId(1), tapClaim = null))
    }
}
