package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.settings.DeleteAfterReadType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MangaDeleteAfterReadTest {

    private lateinit var preferences: PreferencesHelper

    @Before
    fun setup() {
        preferences = mockk(relaxed = true)
    }

    private fun globals(slots: Int, markedAsRead: Boolean) {
        every { preferences.removeAfterReadSlots().get() } returns slots
        every { preferences.removeAfterMarkedAsRead().get() } returns markedAsRead
    }

    private fun manga(type: DeleteAfterReadType) =
        MangaImpl().apply {
            viewer_flags = 0
            deleteAfterReadType = type.flagValue
        }

    @Test
    fun `given default when global is set then uses global slots and toggle`() {
        globals(slots = 2, markedAsRead = true)
        val manga = manga(DeleteAfterReadType.DEFAULT)

        assertEquals(2, manga.removeAfterReadSlots(preferences))
        assertTrue(manga.removeAfterMarkedAsRead(preferences))
    }

    @Test
    fun `given never when global is set then never deletes`() {
        globals(slots = 2, markedAsRead = true)
        val manga = manga(DeleteAfterReadType.NEVER)

        assertEquals(-1, manga.removeAfterReadSlots(preferences))
        assertFalse(manga.removeAfterMarkedAsRead(preferences))
    }

    @Test
    fun `given always when global is set then uses global slots`() {
        globals(slots = 3, markedAsRead = false)
        val manga = manga(DeleteAfterReadType.ALWAYS)

        assertEquals(3, manga.removeAfterReadSlots(preferences))
        assertTrue(manga.removeAfterMarkedAsRead(preferences))
    }

    @Test
    fun `given always when global is never then deletes the last read chapter`() {
        globals(slots = -1, markedAsRead = false)
        val manga = manga(DeleteAfterReadType.ALWAYS)

        assertEquals(0, manga.removeAfterReadSlots(preferences))
        assertTrue(manga.removeAfterMarkedAsRead(preferences))
    }

    @Test
    fun `given unset viewer flags then behaves as default`() {
        globals(slots = 1, markedAsRead = false)
        val manga = MangaImpl()

        assertEquals(-1, manga.viewer_flags)
        assertEquals(1, manga.removeAfterReadSlots(preferences))
        assertFalse(manga.removeAfterMarkedAsRead(preferences))
    }

    @Test
    fun `setting the type keeps the reading mode and orientation bits`() {
        val manga = MangaImpl().apply { viewer_flags = 0b111111 }

        manga.deleteAfterReadType = DeleteAfterReadType.NEVER.flagValue

        assertEquals(0b111111, manga.viewer_flags and 0b111111)
        assertEquals(DeleteAfterReadType.NEVER.flagValue, manga.deleteAfterReadType)
    }
}
