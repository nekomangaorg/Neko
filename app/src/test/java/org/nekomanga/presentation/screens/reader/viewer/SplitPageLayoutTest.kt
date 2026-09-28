package org.nekomanga.presentation.screens.reader.viewer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitPageLayoutTest {

    @Test
    fun `shouldShowLeftHalf in LTR mode shows left half for firstHalf and right half for secondHalf`() {
        // In LTR reading mode:
        // firstHalf = true is the first encountered half (left side) -> should show left half
        assertTrue(shouldShowLeftHalf(firstHalf = true, isRtl = false))

        // firstHalf = false is the second half (right side) -> should NOT show left half (shows
        // right)
        assertFalse(shouldShowLeftHalf(firstHalf = false, isRtl = false))
    }

    @Test
    fun `shouldShowLeftHalf in RTL mode shows right half for firstHalf and left half for secondHalf`() {
        // In Japanese / RTL reading mode:
        // Reading starts on the right, so firstHalf = true is the right side -> should NOT show
        // left half
        assertFalse(shouldShowLeftHalf(firstHalf = true, isRtl = true))

        // Reading continues to the left, so firstHalf = false is the left side -> should show left
        // half
        assertTrue(shouldShowLeftHalf(firstHalf = false, isRtl = true))
    }
}
