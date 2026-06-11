package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter

class SourceChapterSorterTest {

    private fun createChapter(
        name: String,
        vol: String = "",
        chapterTxt: String = "",
        scanlator: String? = null,
        chapterNumber: Float = 0f
    ): Chapter {
        return ChapterImpl().apply {
            this.url = "http://example.com/chapter/$name"
            this.name = name
            this.vol = vol
            this.chapter_txt = chapterTxt
            this.scanlator = scanlator
            this.chapter_number = chapterNumber
        }
    }

    private fun createChapterItem(
        name: String,
        vol: String = "",
        chapterTxt: String = "",
        scanlator: String? = null,
        chapterNumber: Float = 0f
    ): ChapterItem {
        return ChapterItem(
            chapter = SimpleChapter(
                id = 1,
                mangaId = 1,
                read = false,
                bookmark = false,
                lastPageRead = 0,
                dateFetch = 0,
                sourceOrder = 1,
                url = "http://example.com/chapter/$name",
                name = name,
                dateUpload = 0,
                chapterNumber = chapterNumber,
                pagesLeft = 0,
                volume = vol,
                chapterText = chapterTxt,
                chapterTitle = "Title",
                language = "en",
                mangaDexChapterId = "1",
                oldMangaDexChapterId = null,
                scanlator = scanlator ?: "",
                uploader = "1",
                isUnavailable = false,
                smartOrder = 1
            )
        )
    }

    @Test
    fun `getVolumeNum parses valid volume numbers`() {
        getVolumeNum(createChapter("Ch. 1", vol = "5")) shouldBe 5
        getVolumeNum(createChapter("Ch. 1", vol = "0")) shouldBe 0
        getVolumeNum(createChapter("Ch. 1", vol = "-1")) shouldBe -1
    }

    @Test
    fun `getVolumeNum returns null for invalid volume numbers`() {
        getVolumeNum(createChapter("Ch. 1", vol = "")) shouldBe null
        getVolumeNum(createChapter("Ch. 1", vol = "Vol. 5")) shouldBe null
        getVolumeNum(createChapter("Ch. 1", vol = "abc")) shouldBe null
    }

    @Test
    fun `getVolumeNum for ChapterItem parses volume correctly`() {
        getVolumeNum(createChapterItem("Ch. 1", vol = "3")) shouldBe 3
        getVolumeNum(createChapterItem("Ch. 1", vol = "xyz")) shouldBe null
    }

    @Test
    fun `getChapterNum parses chapter numbers correctly from various prefixes`() {
        getChapterNum(createChapter("Name", chapterTxt = "Ch. 5")) shouldBe 5f
        getChapterNum(createChapter("Name", chapterTxt = "Chp. 10.5")) shouldBe 10.5f
        getChapterNum(createChapter("Name", chapterTxt = "Chapter 123")) shouldBe 123f
        getChapterNum(createChapter("Name", chapterTxt = "Ch.  12")) shouldBe 12f
        getChapterNum(createChapter("Name", chapterTxt = "No prefix")) shouldBe null
    }

    @Test
    fun `getChapterNum parses oneshot correctly based on merged status`() {
        // Name contains oneshot and is not merged -> returns 0f
        getChapterNum(createChapter("Oneshot title", scanlator = "NormalScanlator")) shouldBe 0f
        getChapterNum(createChapter("oneshot", scanlator = "NormalScanlator")) shouldBe 0f

        // Name contains oneshot but IS merged -> parses from text instead of returning 0f
        // Let's use "Toonily" as it contains a merge source name (part of MergeType)
        getChapterNum(createChapter("Oneshot title", chapterTxt = "Ch. 1", scanlator = "Toonily")) shouldBe 1f
        getChapterNum(createChapter("oneshot", chapterTxt = "Ch. 2.5", scanlator = "Toonily")) shouldBe 2.5f
    }

    @Test
    fun `getChapterNum for ChapterItem parses chapter number correctly`() {
        getChapterNum(createChapterItem("Name", chapterTxt = "Ch. 7")) shouldBe 7f
        getChapterNum(createChapterItem("oneshot", scanlator = "Normal")) shouldBe 0f
    }

    @Test
    fun `mergeSorted merges multiple sorted lists using a comparator`() {
        val list1 = listOf(4, 2)
        val list2 = listOf(3, 1)
        val lists = listOf(list1, list2)
        val comp = compareBy<Int> { it }
        lists.mergeSorted(comp) shouldBe listOf(4, 3, 2, 1)
    }

    @Test
    fun `reorderChapters partitions specials to the top and sorts them descending by volume`() {
        val specials = listOf(
            createChapter("Special 1", vol = "1"),
            createChapter("Special 2", vol = "3"),
            createChapter("Special 3", vol = "")
        )
        val normal = listOf(
            createChapter("Chapter 1", vol = "1", chapterTxt = "Ch. 1")
        )
        
        val reordered = reorderChapters(normal + specials)
        
        // Specials should be at the top:
        // Sorted descending by volume (vol 3, vol 1, vol null/invalid)
        reordered[0].name shouldBe "Special 2"
        reordered[1].name shouldBe "Special 1"
        reordered[2].name shouldBe "Special 3"
        reordered[3].name shouldBe "Chapter 1"
    }

    @Test
    fun `reorderChapters partitions zero volume chapters to the end`() {
        val normal = listOf(
            createChapter("Chapter 2", vol = "1", chapterTxt = "Ch. 2")
        )
        val zeroVol = listOf(
            createChapter("Chapter 0.2", vol = "0", chapterTxt = "Ch. 0.2"),
            createChapter("Chapter 0.1", vol = "0", chapterTxt = "Ch. 0.1"),
            createChapter("Chapter 0.3", vol = "0", chapterTxt = "")
        )
        
        val reordered = reorderChapters(zeroVol + normal)
        
        // Zero volumes at the end, sorted descending with nulls first
        reordered[0].name shouldBe "Chapter 2"
        // 0-volumes:
        // null first, then descending
        reordered[1].name shouldBe "Chapter 0.3"
        reordered[2].name shouldBe "Chapter 0.2"
        reordered[3].name shouldBe "Chapter 0.1"
    }

    @Test
    fun `reorderChapters when hasVolumeChange is false merges null volume and with volume chapters`() {
        // hasVolumeChange is false if chapter numbers do not reset (e.g. they are sequential)
        // Vol 2: Ch. 3, 4
        // Vol 1: Ch. 1, 2
        // Null vol: Ch. 2.5
        val chapters = listOf(
            createChapter("Ch 4", vol = "2", chapterTxt = "Ch. 4"),
            createChapter("Ch 3", vol = "2", chapterTxt = "Ch. 3"),
            createChapter("Ch 2", vol = "1", chapterTxt = "Ch. 2"),
            createChapter("Ch 1", vol = "1", chapterTxt = "Ch. 1"),
            createChapter("Ch 2.5", vol = "", chapterTxt = "Ch. 2.5")
        )
        
        val reordered = reorderChapters(chapters)
        
        // Since no volume change reset, null volumes and with volumes are merged sorted.
        // Sorting should be descending: Ch 4, Ch 3, Ch 2.5, Ch 2, Ch 1
        reordered.map { it.name } shouldBe listOf("Ch 4", "Ch 3", "Ch 2.5", "Ch 2", "Ch 1")
    }

    @Test
    fun `reorderChapters when hasVolumeChange is true partitions volume 1 to merge with null volumes`() {
        // hasVolumeChange is true if chapter numbers reset (e.g. Vol 2 has Ch. 1-2, Vol 1 has Ch. 1-2)
        // Null vol: Ch. 1.5
        val chapters = listOf(
            createChapter("V2 Ch 2", vol = "2", chapterTxt = "Ch. 2"),
            createChapter("V2 Ch 1", vol = "2", chapterTxt = "Ch. 1"),
            createChapter("V1 Ch 2", vol = "1", chapterTxt = "Ch. 2"),
            createChapter("V1 Ch 1", vol = "1", chapterTxt = "Ch. 1"),
            createChapter("VNull Ch 1.5", vol = "", chapterTxt = "Ch. 1.5")
        )
        
        val reordered = reorderChapters(chapters)
        
        // If hasVolumeChange is true, return order is:
        // specials + withVolume (excluding Vol 1) + merged(nullVolume, Vol 1) + zeroVolume
        // Here, withVolume (excluding Vol 1) is Vol 2: V2 Ch 2, V2 Ch 1.
        // Vol 1 (V1 Ch 2, V1 Ch 1) is merged with nullVolume (VNull Ch 1.5).
        // Merged descending: V1 Ch 2, VNull Ch 1.5, V1 Ch 1.
        // So overall order should be:
        // V2 Ch 2, V2 Ch 1, V1 Ch 2, VNull Ch 1.5, V1 Ch 1.
        reordered.map { it.name } shouldBe listOf("V2 Ch 2", "V2 Ch 1", "V1 Ch 2", "VNull Ch 1.5", "V1 Ch 1")
    }

    @Test
    fun `reorderChapters handles dot chapters with same whole part as no volume change`() {
        // Vol 2: Ch. 1.5
        // Vol 1: Ch. 1.8
        // Null vol: Ch. 1.7
        // Here, first = 1.5, second = 1.8. first < second is true.
        // But floor(first) == floor(second) == 1, and first/second are dot chapters.
        // So it should NOT be considered a volume change.
        val chapters = listOf(
            createChapter("V2 Ch 1.5", vol = "2", chapterTxt = "Ch. 1.5"),
            createChapter("V1 Ch 1.8", vol = "1", chapterTxt = "Ch. 1.8"),
            createChapter("VNull Ch 1.7", vol = "", chapterTxt = "Ch. 1.7")
        )

        val reordered = reorderChapters(chapters)

        // Merged: V2 Ch 1.5 (vol 2), V1 Ch 1.8 (vol 1), VNull Ch 1.7 (null vol)
        reordered.map { it.name } shouldBe listOf("V2 Ch 1.5", "V1 Ch 1.8", "VNull Ch 1.7")
    }

    @Test
    fun `getChapterNum for local manga title parses chapter number correctly`() {
        val chapter = createChapter(
            name = "Local_My Title - 001 - my chapter title (Digital) (i.like.stuff).cbz",
            chapterTxt = "Local_My Title - 001 - my chapter title (Digital) (i.like.stuff).cbz"
        )
        getChapterNum(chapter) shouldBe 1f

        val chapter2 = createChapter(
            name = "Local_My Title - 023.5 - my chapter title.zip",
            chapterTxt = "Local_My Title - 023.5 - my chapter title.zip"
        )
        getChapterNum(chapter2) shouldBe 23.5f

        val chapterWithoutCbz = createChapter(
            name = "Local_My Title - 005 - my chapter title",
            chapterTxt = "Local_My Title - 005 - my chapter title"
        )
        getChapterNum(chapterWithoutCbz) shouldBe 5f
    }
}
