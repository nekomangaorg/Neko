package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.MergeType
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMangaTest {

    private fun row(vararg merges: BackupMergeManga) =
        BackupManga(source = 1L, url = "/manga/x", mergeMangaList = merges.toList())

    private fun merge(type: MergeType, url: String, coverUrl: String = "") =
        BackupMergeManga(url = url, coverUrl = coverUrl, mergeType = type.id)

    private fun restoredMerges(rows: List<BackupManga>) =
        rows.combineRows().getMergeMangaImpl().map { it.mergeType to it.url }

    @Test
    fun `merge on a later row of a title is kept`() {
        val rows = listOf(row(), row(merge(MergeType.Toonily, "/toonily/a")))

        assertEquals(listOf(MergeType.Toonily to "/toonily/a"), restoredMerges(rows))
    }

    @Test
    fun `source that allows several merges keeps each url once`() {
        val rows =
            listOf(
                row(merge(MergeType.Komga, "/komga/a")),
                row(
                    merge(MergeType.Komga, "/komga/b"),
                    merge(MergeType.Komga, "/komga/a", coverUrl = "/changed.png"),
                ),
            )

        assertEquals(
            listOf(MergeType.Komga to "/komga/a", MergeType.Komga to "/komga/b"),
            restoredMerges(rows),
        )
    }

    @Test
    fun `source that allows one merge keeps the first row's merge`() {
        val rows =
            listOf(
                row(merge(MergeType.Toonily, "/toonily/a")),
                row(merge(MergeType.Toonily, "/toonily/b")),
            )

        assertEquals(listOf(MergeType.Toonily to "/toonily/a"), restoredMerges(rows))
    }

    @Test
    fun `first row gives the details and the other lists come from every row`() {
        val first =
            row()
                .copy(
                    title = "first",
                    favorite = true,
                    categories = listOf(1),
                    history = listOf(BackupHistory("/chapter/a", lastRead = 10L)),
                )
        val second =
            row()
                .copy(
                    title = "second",
                    favorite = false,
                    categories = listOf(1, 2),
                    history = listOf(BackupHistory("/chapter/b", lastRead = 20L)),
                )

        val combined = listOf(first, second).combineRows()

        assertEquals("first", combined.title)
        assertEquals(true, combined.favorite)
        assertEquals(listOf(1, 2), combined.categories)
        assertEquals(
            listOf(
                BackupHistory("/chapter/a", lastRead = 10L),
                BackupHistory("/chapter/b", lastRead = 20L),
            ),
            combined.history,
        )
    }
}
