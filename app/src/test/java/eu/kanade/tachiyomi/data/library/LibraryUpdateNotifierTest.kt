package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.core.security.SecurityPreferences
import tachiyomi.core.preference.Preference
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class LibraryUpdateNotifierTest {

    private lateinit var context: Context

    /** Posted notifications by (tag, id), the pair Android uses to tell them apart. */
    private val active = mutableMapOf<Pair<String?, Int>, Notification>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        val hideContent = mockk<Preference<Boolean>> { every { get() } returns false }
        Injekt.addSingleton(
            mockk<SecurityPreferences> { every { hideNotificationContent() } returns hideContent },
        )

        // notify(id) is notify(null, id).
        val notificationManager = mockk<NotificationManager>()
        every { notificationManager.notify(any(), any(), any()) } answers
            {
                active[firstArg<String?>() to secondArg<Int>()] = thirdArg()
            }
        every { notificationManager.notify(any(), any()) } answers
            {
                active[null to firstArg<Int>()] = secondArg()
            }

        context = mockk(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        mockkConstructor(NotificationCompat.Builder::class)
        every { anyConstructed<NotificationCompat.Builder>().build() } answers
            {
                mockk<Notification>(relaxed = true)
            }

        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns
            PackageManager.PERMISSION_GRANTED
        every { ContextCompat.getColor(any(), any()) } returns 0

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeResource(any(), any()) } returns mockk(relaxed = true)

        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setFlags(any()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().setAction(any()) } returns mockk(relaxed = true)
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns
            mockk(relaxed = true)

        mockkObject(NotificationReceiver.Companion)
        every {
            NotificationReceiver.openChapterPendingActivity(any(), any(), any<Chapter>())
        } returns mockk(relaxed = true)
        every { NotificationReceiver.openChapterPendingActivity(any(), any(), any<Int>()) } returns
            mockk(relaxed = true)
        every {
            NotificationReceiver.markAsReadPendingBroadcast(any(), any(), any(), any())
        } returns mockk(relaxed = true)
        every {
            NotificationReceiver.downloadChaptersPendingBroadcast(any(), any(), any(), any())
        } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `new chapter notifications leave app notifications with the same id alone`() = runTest {
        val appIds =
            listOf(
                Notifications.ID_UPDATER,
                Notifications.ID_DOWNLOAD_IMAGE,
                Notifications.ID_INSTALL,
                Notifications.Id.Authentication.SessionExpired,
            )
        for (appId in appIds) {
            active.clear()
            val appNotification = mockk<Notification>(relaxed = true)
            active[null to appId] = appNotification
            val mangaId = appId.toLong()

            LibraryUpdateNotifier(context)
                .showResultNotification(mapOf(libraryManga(mangaId) to arrayOf(chapter(mangaId))))

            withClue("app notification $appId") {
                active[null to appId] shouldBeSameInstanceAs appNotification
                // The summary and the manga's own notification are posted as well.
                active.size shouldBe 3
            }
        }
    }

    private fun libraryManga(mangaId: Long) =
        LibraryManga().apply {
            id = mangaId
            url = "/title/$mangaId"
            title = "Manga $mangaId"
        }

    private fun chapter(mangaId: Long) =
        Chapter.create().apply {
            id = mangaId * 10
            manga_id = mangaId
            url = "/chapter/$mangaId"
            name = "Chapter 1"
        }
}
