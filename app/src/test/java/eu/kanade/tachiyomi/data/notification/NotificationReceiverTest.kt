package eu.kanade.tachiyomi.data.notification

import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class NotificationReceiverTest {

    private lateinit var context: Context

    /** Active notifications by (tag, id), the pair Android uses to tell them apart. */
    private val active = linkedMapOf<Pair<String?, Int>, String>()

    private val appUpdate = null to Notifications.ID_UPDATER
    private val summary = null to Notifications.ID_NEW_CHAPTERS
    private val manga1 = Notifications.TAG_NEW_CHAPTERS to 1
    private val manga5 = Notifications.TAG_NEW_CHAPTERS to 5

    @Before
    fun setup() {
        val notificationManager = mockk<NotificationManager>()
        every { notificationManager.activeNotifications } answers
            {
                active
                    .map { (key, group) ->
                        mockk<StatusBarNotification> {
                            every { tag } returns key.first
                            every { id } returns key.second
                            every { groupKey } returns group
                        }
                    }
                    .toTypedArray()
            }
        // cancel(id) is cancel(null, id).
        every { notificationManager.cancel(any()) } answers
            {
                active.remove(null to firstArg<Int>())
            }
        every { notificationManager.cancel(any(), any()) } answers
            {
                active.remove(firstArg<String?>() to secondArg<Int>())
            }

        context = mockk(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        // An ungrouped notification's group key is its own key.
        active[appUpdate] = "0|org.nekomanga.neko|1|null|10000"
        active[summary] = NEW_CHAPTERS_GROUP
    }

    @Test
    fun `dismissing a new chapter notification leaves the app notification with its id`() {
        active[manga1] = NEW_CHAPTERS_GROUP
        active[manga5] = NEW_CHAPTERS_GROUP

        NotificationReceiver.dismissNotification(context, 1, Notifications.ID_NEW_CHAPTERS)

        active.keys shouldBe setOf(appUpdate, summary, manga5)
    }

    @Test
    fun `dismissing the last new chapter notification cancels the summary only`() {
        active[manga1] = NEW_CHAPTERS_GROUP

        NotificationReceiver.dismissNotification(context, 1, Notifications.ID_NEW_CHAPTERS)

        // Android cancels the group's children with the summary; this fake does not.
        active.keys shouldBe setOf(appUpdate, manga1)
    }

    @Test
    fun `dismissing an app notification leaves the new chapter notification with its id`() {
        active[manga1] = NEW_CHAPTERS_GROUP

        NotificationReceiver.dismissNotification(context, Notifications.ID_UPDATER)

        active.keys shouldBe setOf(summary, manga1)
    }

    private companion object {
        const val NEW_CHAPTERS_GROUP = "0|org.nekomanga.neko|g:org.nekomanga.NEW_CHAPTERS"
    }
}
