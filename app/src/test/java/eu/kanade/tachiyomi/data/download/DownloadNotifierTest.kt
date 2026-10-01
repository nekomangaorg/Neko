package eu.kanade.tachiyomi.data.download

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class DownloadNotifierTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var securityPreferences: SecurityPreferences
    private lateinit var hideNotificationPref: Preference<Boolean>
    private lateinit var mockBuilder: NotificationCompat.Builder
    private lateinit var mockCompleteBuilder: NotificationCompat.Builder
    private lateinit var mockNotification: Notification

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        context = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        securityPreferences = mockk(relaxed = true)
        hideNotificationPref = mockk(relaxed = true)
        mockBuilder = mockk(relaxed = true)
        mockCompleteBuilder = mockk(relaxed = true)
        mockNotification = mockk(relaxed = true)

        every { hideNotificationPref.get() } returns false
        every { securityPreferences.hideNotificationContent() } returns hideNotificationPref
        Injekt.addSingleton(securityPreferences)

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeResource(any(), any()) } returns mockk(relaxed = true)

        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { any<Context>().notificationManager } returns notificationManager

        every { any<Context>().notificationBuilder(any(), any()) } answers
            {
                val block = lastArg<NotificationCompat.Builder.() -> Unit>()
                mockBuilder.apply(block)
                mockBuilder
            }

        every { mockBuilder.build() } returns mockNotification
        every { mockCompleteBuilder.build() } returns mockNotification

        mockkObject(NotificationHandler)
        every { NotificationHandler.openDownloadManagerPendingActivity(any()) } returns
            mockk(relaxed = true)

        every { context.getString(R.string.reindex_downloads) } returns "Reindex downloads"
        every { context.getString(R.string.reindex_downloads_invalidate) } returns
            "Reindexing downloads"
        every { context.getString(R.string.reindex_downloads_complete) } returns
            "Reindexing complete"
        every { context.getString(R.string.reindexing_downloads_progress, any(), any()) } returns
            "Reindexing (1/10)"
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `showReindexProgress notifies with ReindexProgress id and sets progress`() {
        val notifier = DownloadNotifier(context)

        notifier.showReindexProgress(1, 10, "One Piece")

        verify { mockBuilder.setContentTitle("One Piece") }
        verify { mockBuilder.setContentText("Reindexing (1/10)") }
        verify { mockBuilder.setProgress(10, 1, false) }
        verify {
            notificationManager.notify(Notifications.Id.Download.ReindexProgress, mockNotification)
        }
    }

    @Test
    fun `showReindexProgress with hideNotificationContent masks title`() {
        every { hideNotificationPref.get() } returns true
        val notifier = DownloadNotifier(context)

        notifier.showReindexProgress(1, 10, "One Piece")

        verify { mockBuilder.setContentTitle("Reindex downloads") }
        verify { mockBuilder.setContentText("Reindexing (1/10)") }
        verify {
            notificationManager.notify(Notifications.Id.Download.ReindexProgress, mockNotification)
        }
    }

    @Test
    fun `dismissReindexProgress cancels ReindexProgress notification`() {
        val notifier = DownloadNotifier(context)

        notifier.dismissReindexProgress()

        verify { notificationManager.cancel(Notifications.Id.Download.ReindexProgress) }
    }

    @Test
    fun `showReindexComplete cancels progress and notifies with ReindexComplete id`() {
        val notifier = DownloadNotifier(context)

        notifier.showReindexComplete()

        verify { notificationManager.cancel(Notifications.Id.Download.ReindexProgress) }
        verify { notificationManager.notify(Notifications.Id.Download.ReindexComplete, any()) }
    }
}
