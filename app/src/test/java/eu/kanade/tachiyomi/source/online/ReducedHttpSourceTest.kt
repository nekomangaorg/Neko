package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.merged.comix.Comix
import eu.kanade.tachiyomi.source.online.merged.kagane.Kagane
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.domain.network.ResultError

class ReducedHttpSourceTest {

    private val source = mockk<ReducedHttpSource> { every { name } returns "Merged source" }
    private val chapterRepository = mockk<ChapterRepository>()
    private val mergeManga =
        MergeMangaImpl(mangaId = MANGA_ID, url = MANGA_URL, mergeType = MergeType.Kagane)

    private fun storedChapters(vararg scanlators: String?) {
        coEvery { chapterRepository.getChaptersForManga(MANGA_ID) } returns
            scanlators.map { value -> Chapter.create().apply { scanlator = value } }
    }

    @Test
    fun `fetchMergedChapters returns the chapters the source found`() = runTest {
        val chapters = listOf(SChapter.create() to false)
        coEvery { source.fetchChapters(MANGA_URL) } returns Ok(chapters)

        source.fetchMergedChapters(mergeManga, chapterRepository).get() shouldBe chapters
    }

    @Test
    fun `fetchMergedChapters skips the stored chapter read when the source found chapters`() =
        runTest {
            coEvery { source.fetchChapters(MANGA_URL) } returns
                Ok(listOf(SChapter.create() to false))

            source.fetchMergedChapters(mergeManga, chapterRepository)

            coVerify(exactly = 0) { chapterRepository.getChaptersForManga(any()) }
        }

    @Test
    fun `fetchMergedChapters passes a source error through`() = runTest {
        val error = ResultError.HttpError(503, "HTTP 503")
        coEvery { source.fetchChapters(MANGA_URL) } returns Err(error)

        source.fetchMergedChapters(mergeManga, chapterRepository).getError() shouldBe error
    }

    @Test
    fun `fetchMergedChapters skips the stored chapter read when the fetch fails`() = runTest {
        coEvery { source.fetchChapters(MANGA_URL) } returns
            Err(ResultError.HttpError(503, "HTTP 503"))

        source.fetchMergedChapters(mergeManga, chapterRepository)

        coVerify(exactly = 0) { chapterRepository.getChaptersForManga(any()) }
    }

    @Test
    fun `fetchMergedChapters returns a thrown exception as an error`() = runTest {
        coEvery { source.fetchChapters(MANGA_URL) } throws IOException("boom")

        source.fetchMergedChapters(mergeManga, chapterRepository).getError() shouldBe
            ResultError.Generic("boom")
    }

    @Test
    fun `fetchMergedChapters rethrows cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        coEvery { source.fetchChapters(MANGA_URL) } throws cancellation

        val thrown = runCatching {
            source.fetchMergedChapters(mergeManga, chapterRepository)
        }
            .exceptionOrNull()

        assertSame(cancellation, thrown)
    }

    @Test
    fun `fetchMergedChapters returns an empty list as an error when that source has stored chapters`() =
        runTest {
            coEvery { source.fetchChapters(MANGA_URL) } returns Ok(emptyList())
            storedChapters("Group A", Kagane.name)

            source.fetchMergedChapters(mergeManga, chapterRepository).getError() shouldBe
                ResultError.Generic(errorRes = R.string.no_chapters_error)
        }

    @Test
    fun `fetchMergedChapters counts a stored scanlator that joins the source with its groups`() =
        runTest {
            coEvery { source.fetchChapters(MANGA_URL) } returns Ok(emptyList())
            storedChapters(
                listOf(Kagane.name, "MangaPlus", "Group A")
                    .joinToString(Constants.SCANLATOR_SEPARATOR)
            )

            source.fetchMergedChapters(mergeManga, chapterRepository).getError() shouldBe
                ResultError.Generic(errorRes = R.string.no_chapters_error)
        }

    @Test
    fun `fetchMergedChapters returns an empty list as ok when the manga has no stored chapters`() =
        runTest {
            coEvery { source.fetchChapters(MANGA_URL) } returns Ok(emptyList())
            storedChapters()

            source.fetchMergedChapters(mergeManga, chapterRepository).get() shouldBe emptyList()
        }

    @Test
    fun `fetchMergedChapters returns an empty list as ok when stored chapters are from elsewhere`() =
        runTest {
            coEvery { source.fetchChapters(MANGA_URL) } returns Ok(emptyList())
            storedChapters(Comix.name, "Group A", null)

            source.fetchMergedChapters(mergeManga, chapterRepository).get() shouldBe emptyList()
        }

    companion object {
        private const val MANGA_ID = 1L
        private const val MANGA_URL = "/series/1"
    }
}
