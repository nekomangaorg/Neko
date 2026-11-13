package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.track.TrackManager
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

    private fun rowWithChapter(chapter: BackupChapter) = row().copy(chapters = listOf(chapter))

    private fun chapter(
        read: Boolean = false,
        bookmark: Boolean = false,
        lastPageRead: Int = 0,
        pagesLeft: Int = 0,
    ) =
        BackupChapter(
            url = "/chapter/a",
            name = "Ch.1",
            read = read,
            bookmark = bookmark,
            lastPageRead = lastPageRead,
            pagesLeft = pagesLeft,
        )

    @Test
    fun `chapter on both rows is restored once with the furthest progress`() {
        val rows =
            listOf(
                rowWithChapter(chapter(bookmark = true, lastPageRead = 3, pagesLeft = 9)),
                rowWithChapter(chapter(lastPageRead = 7, pagesLeft = 5)),
            )

        val combined = rows.combineRows().chapters.single()

        assertEquals(false, combined.read)
        assertEquals(true, combined.bookmark)
        assertEquals(7, combined.lastPageRead)
        assertEquals(5, combined.pagesLeft)
    }

    @Test
    fun `chapter read on one row has no pages left`() {
        val rows =
            listOf(
                rowWithChapter(chapter(read = true)),
                rowWithChapter(chapter(lastPageRead = 5, pagesLeft = 12)),
            )

        val combined = rows.combineRows().chapters.single()

        assertEquals(true, combined.read)
        assertEquals(5, combined.lastPageRead)
        assertEquals(0, combined.pagesLeft)
    }

    @Test
    fun `title stays in the library when only a later row is a favorite`() {
        val rows = listOf(row().copy(favorite = false), row().copy(favorite = true))

        assertEquals(true, rows.combineRows().favorite)
    }

    @Test
    fun `row with a source gives the details when a row without one comes first`() {
        val rows = listOf(row().copy(source = -1L, title = "lost"), row().copy(title = "kept"))

        assertEquals("kept", rows.combineRows().title)
    }

    @Test
    fun `user settings on a later row are kept when the first row lost its source`() {
        val rows =
            listOf(
                row().copy(source = -1L, customTitle = " ", viewer_flags = 0),
                row()
                    .copy(
                        customTitle = "My title",
                        alternativeArtwork = "/art.png",
                        scanlatorFilter = "Group A",
                        viewer_flags = 1,
                        chapterFlags = 4353,
                    ),
            )

        val combined = rows.combineRows()

        assertEquals("My title", combined.customTitle)
        assertEquals("/art.png", combined.alternativeArtwork)
        assertEquals("Group A", combined.scanlatorFilter)
        assertEquals(1, combined.viewer_flags)
        assertEquals(4353, combined.chapterFlags)
    }

    @Test
    fun `settings on the row without a source are kept over the empty ones of its copy`() {
        val rows =
            listOf(
                row()
                    .copy(
                        source = -1L,
                        customTitle = "My title",
                        alternativeArtwork = "/art.png",
                        scanlatorFilter = "Group A",
                        viewer_flags = 3,
                        chapterFlags = 64,
                    ),
                row().copy(viewer_flags = 0, chapterFlags = 0),
            )

        val combined = rows.combineRows()

        assertEquals("My title", combined.customTitle)
        assertEquals("/art.png", combined.alternativeArtwork)
        assertEquals("Group A", combined.scanlatorFilter)
        assertEquals(3, combined.viewer_flags)
        assertEquals(64, combined.chapterFlags)
    }

    @Test
    fun `reader mode on a row from before viewer flags existed is kept`() {
        val rows = listOf(row().copy(source = -1L, viewer = 2), row())

        assertEquals(2, rows.combineRows().getMangaImpl().viewer_flags)
    }

    @Test
    fun `rows that all have a date keep the earliest and ignore chapter fetches`() {
        val rows =
            listOf(
                row()
                    .copy(
                        dateAdded = 20L,
                        chapters =
                            listOf(BackupChapter(url = "/chapter/a", name = "", dateFetch = 5L)),
                    ),
                row().copy(dateAdded = 10L),
            )

        assertEquals(10L, rows.combineRows().dateAdded)
    }

    @Test
    fun `row with no date takes the earliest chapter fetch over the date of its copy`() {
        val rows =
            listOf(
                row()
                    .copy(
                        source = -1L,
                        chapters =
                            listOf(
                                BackupChapter(url = "/chapter/a", name = "", dateFetch = 8L),
                                BackupChapter(url = "/chapter/b", name = "", dateFetch = 5L),
                            ),
                    ),
                row().copy(dateAdded = 20L),
            )

        assertEquals(5L, rows.combineRows().dateAdded)
    }

    @Test
    fun `row with no date and no chapters takes the date of its copy`() {
        val rows = listOf(row().copy(source = -1L), row().copy(dateAdded = 20L))

        assertEquals(20L, rows.combineRows().dateAdded)
    }

    @Test
    fun `rows with no date and no chapters stay at zero`() {
        assertEquals(0L, listOf(row(), row()).combineRows().dateAdded)
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

    private fun track(
        syncId: Int = TrackManager.MDLIST,
        title: String = "",
        lastChapterRead: Float = 0f,
    ) =
        BackupTracking(
            syncId = syncId,
            libraryId = -1L,
            title = title,
            lastChapterRead = lastChapterRead,
        )

    @Test
    fun `tracker on both rows takes the furthest chapter and the details of the row with a source`() {
        val rows =
            listOf(
                row()
                    .copy(
                        source = -1L,
                        tracking = listOf(track(title = "old", lastChapterRead = 12f)),
                    ),
                row().copy(tracking = listOf(track(title = "new"))),
            )

        assertEquals(
            listOf(track(title = "new", lastChapterRead = 12f)),
            rows.combineRows().tracking,
        )
    }

    @Test
    fun `tracker on both rows takes the details of the row with a source when it comes first`() {
        val rows =
            listOf(
                row().copy(tracking = listOf(track(title = "new"))),
                row()
                    .copy(
                        source = -1L,
                        tracking = listOf(track(title = "old", lastChapterRead = 12f)),
                    ),
            )

        assertEquals(
            listOf(track(title = "new", lastChapterRead = 12f)),
            rows.combineRows().tracking,
        )
    }

    @Test
    fun `tracker the row with a source lacks takes the details of the first row that has it`() {
        val rows =
            listOf(
                row()
                    .copy(
                        source = -1L,
                        tracking = listOf(track(title = "first", lastChapterRead = 5f)),
                    ),
                row()
                    .copy(
                        source = -1L,
                        tracking = listOf(track(title = "second", lastChapterRead = 9f)),
                    ),
                row(),
            )

        assertEquals(
            listOf(track(title = "first", lastChapterRead = 9f)),
            rows.combineRows().tracking,
        )
    }

    @Test
    fun `trackers of different services on the rows are each kept`() {
        val mal = track(syncId = TrackManager.MYANIMELIST, lastChapterRead = 7f)
        val rows =
            listOf(
                row().copy(source = -1L, tracking = listOf(track(lastChapterRead = 3f))),
                row().copy(tracking = listOf(mal)),
            )

        assertEquals(listOf(track(lastChapterRead = 3f), mal), rows.combineRows().tracking)
    }

    @Test
    fun `chapter read on both rows keeps the latest read and the longest reading time`() {
        val rows =
            listOf(
                row()
                    .copy(
                        source = -1L,
                        history =
                            listOf(
                                BackupHistory("/chapter/a", lastRead = 200L, readDuration = 5L),
                                BackupHistory("/chapter/b", lastRead = 100L, readDuration = 9L),
                            ),
                    ),
                row()
                    .copy(
                        history =
                            listOf(
                                BackupHistory("/chapter/a", lastRead = 100L, readDuration = 9L),
                                BackupHistory("/chapter/b", lastRead = 200L, readDuration = 5L),
                            ),
                    ),
            )

        assertEquals(
            listOf(
                BackupHistory("/chapter/a", lastRead = 200L, readDuration = 9L),
                BackupHistory("/chapter/b", lastRead = 200L, readDuration = 9L),
            ),
            rows.combineRows().history,
        )
    }
}
