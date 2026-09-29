package eu.kanade.tachiyomi.crash

import android.app.Activity
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleActivityTrackerTest {

    private val activity = mockk<Activity>(relaxed = true)

    @Test
    fun `no activity is visible before the first start`() {
        assertFalse(VisibleActivityTracker().isActivityVisible)
    }

    @Test
    fun `an activity is visible from start until stop`() {
        val tracker = VisibleActivityTracker()

        tracker.onActivityStarted(activity)
        assertTrue(tracker.isActivityVisible)
        tracker.onActivityStopped(activity)
        assertFalse(tracker.isActivityVisible)
    }

    @Test
    fun `stays visible while a second started activity is still started`() {
        val tracker = VisibleActivityTracker()

        tracker.onActivityStarted(activity)
        tracker.onActivityStarted(activity)
        tracker.onActivityStopped(activity)

        assertTrue(tracker.isActivityVisible)
    }

    @Test
    fun `a stop without a start does not hide a later start`() {
        val tracker = VisibleActivityTracker()

        tracker.onActivityStopped(activity)
        tracker.onActivityStarted(activity)

        assertTrue(tracker.isActivityVisible)
    }
}
