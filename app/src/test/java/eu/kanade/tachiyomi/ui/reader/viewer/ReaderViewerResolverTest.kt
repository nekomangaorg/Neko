package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ReaderViewerResolverTest {

    private val created = mutableListOf<Pair<Int, Boolean>>()

    private fun webtoon(noWebtoonTag: Boolean): WebtoonViewer =
        mockk<WebtoonViewer>().also { every { it.noWebtoonTag } returns noWebtoonTag }

    private fun create(readingMode: Int, noWebtoonTag: Boolean): BaseViewer {
        created += readingMode to noWebtoonTag
        return when (readingMode) {
            ReadingModeType.LEFT_TO_RIGHT.flagValue -> mockk<L2RPagerViewer>()
            ReadingModeType.VERTICAL.flagValue -> mockk<VerticalPagerViewer>()
            ReadingModeType.WEBTOON.flagValue -> webtoon(noWebtoonTag)
            else -> mockk<R2LPagerViewer>()
        }
    }

    private fun resolve(current: BaseViewer?, readingMode: Int, noWebtoonTag: Boolean = false) =
        ReaderViewerResolver.resolve(current, readingMode, noWebtoonTag, ::create)

    @Test
    fun `matching pager viewer is kept without building another`() {
        val cases =
            listOf(
                mockk<L2RPagerViewer>() to ReadingModeType.LEFT_TO_RIGHT.flagValue,
                mockk<R2LPagerViewer>() to ReadingModeType.RIGHT_TO_LEFT.flagValue,
                mockk<R2LPagerViewer>() to ReadingModeType.DEFAULT.flagValue,
                mockk<VerticalPagerViewer>() to ReadingModeType.VERTICAL.flagValue,
            )
        cases.forEach { (current, readingMode) ->
            assertSame(current, resolve(current, readingMode))
        }
        assertEquals(emptyList<Pair<Int, Boolean>>(), created)
    }

    @Test
    fun `matching webtoon viewer is kept without building another`() {
        val current = webtoon(noWebtoonTag = true)

        assertSame(
            current,
            resolve(current, ReadingModeType.WEBTOON.flagValue, noWebtoonTag = true),
        )
        assertEquals(emptyList<Pair<Int, Boolean>>(), created)
    }

    @Test
    fun `webtoon viewer with a different tag is replaced`() {
        val current = webtoon(noWebtoonTag = false)

        val result = resolve(current, ReadingModeType.WEBTOON.flagValue, noWebtoonTag = true)

        assertEquals(true, (result as WebtoonViewer).noWebtoonTag)
        assertEquals(listOf(ReadingModeType.WEBTOON.flagValue to true), created)
    }

    @Test
    fun `different reading mode builds a new viewer`() {
        val result = resolve(mockk<L2RPagerViewer>(), ReadingModeType.RIGHT_TO_LEFT.flagValue)

        assertEquals(true, result is R2LPagerViewer)
        assertEquals(listOf(ReadingModeType.RIGHT_TO_LEFT.flagValue to false), created)
    }

    @Test
    fun `no current viewer builds one`() {
        val result = resolve(null, ReadingModeType.VERTICAL.flagValue)

        assertEquals(true, result is VerticalPagerViewer)
        assertEquals(listOf(ReadingModeType.VERTICAL.flagValue to false), created)
    }
}
