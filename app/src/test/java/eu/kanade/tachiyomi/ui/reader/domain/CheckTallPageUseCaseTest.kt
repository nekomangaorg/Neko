package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckTallPageUseCaseTest {

    private val useCase = CheckTallPageUseCase()

    private fun createPage(): ReaderPage {
        val dbChapter =
            Chapter.create().apply {
                id = 1L
                url = "/chapter/1"
                name = "Chapter 1"
            }
        val readerChapter = ReaderChapter(dbChapter)
        return ReaderPage(0, "url_0", "img_0").apply { chapter = readerChapter }
    }

    @Test
    fun `computeSplits returns null for non-positive dimensions`() {
        val page = createPage()
        assertNull(useCase.computeSplits(page, outWidth = 0, outHeight = 1000, screenHeight = 1000))
        assertNull(useCase.computeSplits(page, outWidth = 1000, outHeight = 0, screenHeight = 1000))
        assertNull(
            useCase.computeSplits(page, outWidth = -100, outHeight = 1000, screenHeight = 1000)
        )
    }

    @Test
    fun `computeSplits returns null for standard aspect ratio images within texture limits`() {
        val page = createPage()
        // 1000 x 1500: aspect ratio = 1.5, well under 3.0 and under maxTextureSize
        val splits =
            useCase.computeSplits(
                page,
                outWidth = 1000,
                outHeight = 1500,
                screenHeight = 1000,
                maxTextureSize = 4096,
            )
        assertNull(splits)
    }

    @Test
    fun `computeSplits returns null when height does not exceed displayMaxHeight`() {
        val page = createPage()
        // Aspect ratio is 4.0 (> 3.0), but height is 2000, which is <= screenHeight * 2 (2000) or
        // min (4096)
        val splits =
            useCase.computeSplits(
                page,
                outWidth = 500,
                outHeight = 2000,
                screenHeight = 1000,
                maxTextureSize = 4096,
            )
        assertNull(splits)
    }

    @Test
    fun `computeSplits splits tall webtoon strips exceeding displayMaxHeight`() {
        val page = createPage()
        // 1000 x 12000 webtoon strip
        val splits =
            useCase.computeSplits(
                page,
                outWidth = 1000,
                outHeight = 12000,
                screenHeight = 1000,
                maxTextureSize = 4096,
            )
        assertNotNull(splits)
        val nonNullSplits = splits!!
        assertEquals(3, nonNullSplits.size)

        // Verifying continuous coverage
        assertEquals(0, nonNullSplits[0].topOffset)
        assertEquals(4000, nonNullSplits[0].splitHeight)

        assertEquals(4000, nonNullSplits[1].topOffset)
        assertEquals(4000, nonNullSplits[1].splitHeight)

        assertEquals(8000, nonNullSplits[2].topOffset)
        assertEquals(4000, nonNullSplits[2].splitHeight)

        val totalHeight = nonNullSplits.sumOf { it.splitHeight }
        assertEquals(12000, totalHeight)

        nonNullSplits.forEach { split ->
            assertEquals(1000f / 4000f, split.aspectRatio!!, 0.001f)
            assertEquals(page, split.page)
        }
    }

    @Test
    fun `computeSplits splits images exceeding maxTextureSize even if aspect ratio is less than 3`() {
        val page = createPage()
        // 3000 x 5000 image (aspect ratio < 3.0), but height 5000 exceeds maxTextureSize 4096
        val splits =
            useCase.computeSplits(
                page,
                outWidth = 3000,
                outHeight = 5000,
                screenHeight = 1000,
                maxTextureSize = 4096,
            )
        assertNotNull(splits)
        assertTrue(splits!!.size >= 2)
        assertEquals(5000, splits.sumOf { it.splitHeight })
    }

    @Test
    fun `invoke returns null when stream is null`() {
        val page = createPage()
        page.stream = null
        val splits = useCase(page, screenHeight = 1000)
        assertNull(splits)
    }

    @Test
    fun `invoke returns cached precomputed splits without invoking stream`() {
        val page = createPage()
        val dummySplits =
            listOf(
                ReaderPageSplit(
                    page = page,
                    topOffset = 0,
                    splitHeight = 2000,
                )
            )
        page.precomputedSplits = dummySplits
        // Stream throws exception if called
        page.stream = { throw IllegalStateException("Stream should not be called") }

        val result = useCase(page, screenHeight = 1000)
        assertEquals(dummySplits, result)
    }

    @Test
    fun `invoke returns null without invoking stream when precomputedSplits is emptyList`() {
        val page = createPage()
        page.precomputedSplits = emptyList()
        page.stream = { throw IllegalStateException("Stream should not be called") }

        val result = useCase(page, screenHeight = 1000)
        assertNull(result)
    }
}
