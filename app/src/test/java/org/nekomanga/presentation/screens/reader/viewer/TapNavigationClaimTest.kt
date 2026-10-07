package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.ui.input.pointer.PointerId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapNavigationClaimTest {

    @Test
    fun `no pointer is claimed before a control claims one`() {
        val claim = TapNavigationClaim()

        assertFalse(claim.isClaimed(PointerId(1)))
    }

    @Test
    fun `claimed pointer is reported as claimed`() {
        val claim = TapNavigationClaim()

        claim.claim(PointerId(1))

        assertTrue(claim.isClaimed(PointerId(1)))
    }

    @Test
    fun `later pointer is not covered by an earlier claim`() {
        val claim = TapNavigationClaim()

        claim.claim(PointerId(1))

        assertFalse(claim.isClaimed(PointerId(2)))
    }

    @Test
    fun `pointers held on two controls are both claimed`() {
        val claim = TapNavigationClaim()

        claim.claim(PointerId(1))
        claim.claim(PointerId(2))

        assertTrue(claim.isClaimed(PointerId(1)))
        assertTrue(claim.isClaimed(PointerId(2)))
    }

    @Test
    fun `released pointer is no longer claimed`() {
        val claim = TapNavigationClaim()

        claim.claim(PointerId(1))
        claim.claim(PointerId(2))
        claim.release(PointerId(1))

        assertFalse(claim.isClaimed(PointerId(1)))
        assertTrue(claim.isClaimed(PointerId(2)))
    }
}
