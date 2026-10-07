package org.nekomanga.presentation.screens.reader.viewer

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
}
