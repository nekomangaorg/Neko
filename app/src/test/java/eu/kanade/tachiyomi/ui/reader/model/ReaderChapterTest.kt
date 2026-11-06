package eu.kanade.tachiyomi.ui.reader.model

import android.util.Log
import eu.kanade.tachiyomi.data.database.models.Chapter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class ReaderChapterTest {

    private val warnings = mutableListOf<String>()

    @Before
    fun setUp() {
        Timber.plant(
            object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority == Log.WARN) warnings += message
                }
            }
        )
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    private fun readerChapter() =
        ReaderChapter(
            Chapter.create().apply {
                url = "/chapter/1"
                name = "Chapter 1"
            }
        )

    @Test
    fun `unref with no references left keeps the count at zero and warns`() {
        // Given a chapter holding one reference
        val chapter = readerChapter().apply { ref() }

        // When it is released once and then once more
        chapter.unref()
        val warningsAtZero = warnings.size
        chapter.unref()

        // Then the count stays at zero and only the extra release warns
        assertEquals(0, chapter.references)
        assertEquals(0, warningsAtZero)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `a chapter released once too often still recycles on its next release`() {
        // Given a chapter released once more than it was referenced
        val chapter = readerChapter().apply { ref() }
        chapter.unref()
        chapter.unref()

        // When it is loaded again and released
        chapter.ref()
        chapter.state = ReaderChapter.State.Loading
        chapter.unref()

        // Then that release recycles it
        assertEquals(ReaderChapter.State.Wait, chapter.state)
    }
}
