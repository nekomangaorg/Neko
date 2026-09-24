package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.model.SChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.constants.Constants

class ChapterSyncUnavailableTest {

    @Test
    fun `available chapter reported unavailable by the source became unavailable`() {
        val dbChapter = Chapter.create().apply { isUnavailable = false }
        val sourceChapter = SChapter.create().apply { isUnavailable = true }

        assertTrue(becameUnavailable(dbChapter, sourceChapter))
    }

    @Test
    fun `chapter already unavailable is not reported again`() {
        val dbChapter = Chapter.create().apply { isUnavailable = true }
        val sourceChapter = SChapter.create().apply { isUnavailable = true }

        assertFalse(becameUnavailable(dbChapter, sourceChapter))
    }

    @Test
    fun `chapter still available is not reported`() {
        val dbChapter = Chapter.create().apply { isUnavailable = false }
        val sourceChapter = SChapter.create().apply { isUnavailable = false }

        assertFalse(becameUnavailable(dbChapter, sourceChapter))
    }

    @Test
    fun `chapter that came back is not reported`() {
        val dbChapter = Chapter.create().apply { isUnavailable = true }
        val sourceChapter = SChapter.create().apply { isUnavailable = false }

        assertFalse(becameUnavailable(dbChapter, sourceChapter))
    }

    @Test
    fun `chapter missing from the source is reported`() {
        val gone = Chapter.create().apply { url = "/chapter/gone" }

        assertEquals(listOf(gone), goneFromSource(listOf(gone), setOf("/chapter/other")))
    }

    @Test
    fun `duplicate row of a chapter the source still lists is not reported`() {
        val dupe = Chapter.create().apply { url = "/chapter/still-there" }

        assertEquals(
            emptyList<Chapter>(),
            goneFromSource(listOf(dupe), setOf("/chapter/still-there")),
        )
    }

    @Test
    fun `chapter already flagged unavailable is not reported when it drops out of the feed`() {
        val flagged =
            Chapter.create().apply {
                url = "/chapter/flagged"
                isUnavailable = true
            }

        assertEquals(emptyList<Chapter>(), goneFromSource(listOf(flagged), emptySet()))
    }

    @Test
    fun `local and merged chapters are not reported`() {
        val local =
            Chapter.create().apply {
                url = "Local/file.cbz"
                scanlator = Constants.LOCAL_SOURCE
                isUnavailable = true
            }
        val merged =
            Chapter.create().apply {
                url = "/merged/1"
                scanlator = MergeType.getMergeTypeName(MergeType.Komga)
            }

        assertEquals(emptyList<Chapter>(), goneFromSource(listOf(local, merged), emptySet()))
    }
}
