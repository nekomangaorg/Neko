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

    @Test
    fun `unref warns only when the references drop below zero`() {
        // Given a chapter holding one reference
        val chapter =
            ReaderChapter(
                    Chapter.create().apply {
                        url = "/chapter/1"
                        name = "Chapter 1"
                    }
                )
                .apply { ref() }

        // When it is released once and then once more
        chapter.unref()
        val warningsAtZero = warnings.size
        chapter.unref()

        // Then only the release below zero warns
        assertEquals(0, warningsAtZero)
        assertEquals(1, warnings.size)
    }
}
