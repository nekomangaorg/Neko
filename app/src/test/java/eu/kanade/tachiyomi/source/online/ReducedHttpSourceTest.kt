package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import eu.kanade.tachiyomi.source.model.SChapter
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.domain.network.ResultError

class ReducedHttpSourceTest {

    private val source = mockk<ReducedHttpSource> { every { name } returns "Merged source" }

    @Test
    fun `fetchMergedChapters returns the chapters the source found`() = runTest {
        val chapters = listOf(SChapter.create() to false)
        coEvery { source.fetchChapters(MANGA_URL) } returns Ok(chapters)

        source.fetchMergedChapters(MANGA_URL).get() shouldBe chapters
    }

    @Test
    fun `fetchMergedChapters passes a source error through`() = runTest {
        val error = ResultError.HttpError(503, "HTTP 503")
        coEvery { source.fetchChapters(MANGA_URL) } returns Err(error)

        source.fetchMergedChapters(MANGA_URL).getError() shouldBe error
    }

    @Test
    fun `fetchMergedChapters returns a thrown exception as an error`() = runTest {
        coEvery { source.fetchChapters(MANGA_URL) } throws IOException("boom")

        source.fetchMergedChapters(MANGA_URL).getError() shouldBe ResultError.Generic("boom")
    }

    @Test
    fun `fetchMergedChapters rethrows cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        coEvery { source.fetchChapters(MANGA_URL) } throws cancellation

        val thrown = runCatching { source.fetchMergedChapters(MANGA_URL) }.exceptionOrNull()

        assertSame(cancellation, thrown)
    }

    @Test
    fun `fetchMergedChapters returns an empty chapter list as an error`() = runTest {
        coEvery { source.fetchChapters(MANGA_URL) } returns Ok(emptyList())

        source.fetchMergedChapters(MANGA_URL).getError() shouldBe
            ResultError.Generic(errorRes = R.string.no_chapters_error)
    }

    companion object {
        private const val MANGA_URL = "/series/1"
    }
}
