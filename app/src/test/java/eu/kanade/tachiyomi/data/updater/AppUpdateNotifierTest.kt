package eu.kanade.tachiyomi.data.updater

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import java.util.IdentityHashMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * `NotificationCompat.Builder.build()` needs the Android framework, so the test stubs it and
 * records whether the builder was set ongoing when each notification was posted.
 */
class AppUpdateNotifierTest {

    private val context = mockk<Context>(relaxed = true)
    private val ongoingWhenPosted = mutableMapOf<Int, Boolean>()

    @Before
    fun setup() {
        AppUpdateNotifier.releasePageUrl = null
        // Kept per builder, so a notification from a fresh builder starts out not ongoing
        val ongoing = IdentityHashMap<Any, Boolean>()
        var ongoingAtBuild = false
        mockkConstructor(NotificationCompat.Builder::class)
        every { anyConstructed<NotificationCompat.Builder>().setOngoing(any()) } answers
            {
                ongoing[self] = firstArg()
                self as NotificationCompat.Builder
            }
        every { anyConstructed<NotificationCompat.Builder>().build() } answers
            {
                ongoingAtBuild = ongoing[self] ?: false
                mockk<Notification>()
            }

        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { notificationManager.notify(any<Int>(), any()) } answers
            {
                ongoingWhenPosted[firstArg()] = ongoingAtBuild
            }
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { context.notificationManager } returns notificationManager

        mockkObject(NotificationReceiver, NotificationHandler)
        every { NotificationReceiver.cancelUpdateDownloadPendingBroadcast(any()) } returns mockk()
        every { NotificationReceiver.dismissNotificationPendingBroadcast(any(), any()) } returns
            mockk()
        every { NotificationReceiver.startAppUpdatePendingJob(any(), any(), any(), any()) } returns
            mockk()
        every { NotificationReceiver.dismissNotification(any(), any(), any()) } just runs
        every { NotificationHandler.installApkPendingActivity(any(), any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onDownloadFinished after onDownloadStarted posts a notification the user can dismiss`() {
        val notifier = AppUpdateNotifier(context)
        notifier.onDownloadStarted()

        notifier.onDownloadFinished(mockk())

        assertEquals(true, ongoingWhenPosted[Notifications.ID_UPDATER])
        assertEquals(false, ongoingWhenPosted[Notifications.ID_INSTALL])
    }

    @Test
    fun `onDownloadError after onDownloadStarted posts a notification the user can dismiss`() {
        val notifier = AppUpdateNotifier(context)
        notifier.onDownloadStarted()

        notifier.onDownloadError("http://127.0.0.1/neko.apk", "v9.9.9")

        assertEquals(true, ongoingWhenPosted[Notifications.ID_UPDATER])
        assertEquals(false, ongoingWhenPosted[Notifications.ID_INSTALL])
    }
}
