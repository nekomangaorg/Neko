package eu.kanade.tachiyomi.crash

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

/**
 * Counts started activities, so the crash handler knows whether it can start the crash screen.
 * Android blocks an activity start from a process that has no visible activity. Chosen over
 * ProcessLifecycleOwner, which reports ON_STOP about 700 ms late.
 */
class VisibleActivityTracker : Application.ActivityLifecycleCallbacks {

    private val startedActivities = AtomicInteger(0)

    val isActivityVisible: Boolean
        get() = startedActivities.get() > 0

    override fun onActivityStarted(activity: Activity) {
        startedActivities.incrementAndGet()
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.updateAndGet { maxOf(0, it - 1) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
