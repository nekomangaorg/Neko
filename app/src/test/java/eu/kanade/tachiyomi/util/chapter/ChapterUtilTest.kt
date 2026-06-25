package eu.kanade.tachiyomi.util.chapter

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.nekomanga.constants.Constants

class ChapterUtilTest {

    @Test
    fun `getScanlators returns empty list for null or blank input`() {
        ChapterUtil.getScanlators(null) shouldBe emptyList()
        ChapterUtil.getScanlators("") shouldBe emptyList()
        ChapterUtil.getScanlators("   ") shouldBe emptyList()
    }

    @Test
    fun `getScanlators returns single item list when no separator is present`() {
        ChapterUtil.getScanlators("Group A") shouldBe listOf("Group A")
    }

    @Test
    fun `getScanlators splits scanlators on separator and returns distinct values`() {
        val input = "Group A${Constants.SCANLATOR_SEPARATOR}Group B${Constants.SCANLATOR_SEPARATOR}Group A"
        ChapterUtil.getScanlators(input) shouldBe listOf("Group A", "Group B")
    }

    @Test
    fun `getLanguages returns empty list for null or blank input`() {
        ChapterUtil.getLanguages(null) shouldBe emptyList()
        ChapterUtil.getLanguages("") shouldBe emptyList()
    }

    @Test
    fun `getLanguages returns single item list when no separator is present`() {
        ChapterUtil.getLanguages("en") shouldBe listOf("en")
    }

    @Test
    fun `getLanguages splits languages on separator and returns distinct values`() {
        val input = "en${Constants.SCANLATOR_SEPARATOR}ja${Constants.SCANLATOR_SEPARATOR}en"
        ChapterUtil.getLanguages(input) shouldBe listOf("en", "ja")
    }

    @Test
    fun `filterByScanlator returns false when filtered collections are empty`() {
        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A"),
            uploader = "",
            all = false,
            filteredGroups = emptySet(),
            filteredUploaders = emptySet()
        ) shouldBe false
    }

    @Test
    fun `filterByScanlator match any returns true if any scanlator is filtered`() {
        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", "Group B"),
            uploader = "",
            all = false,
            filteredGroups = setOf("Group B"),
            filteredUploaders = emptySet()
        ) shouldBe true

        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", "Group B"),
            uploader = "",
            all = false,
            filteredGroups = setOf("Group C"),
            filteredUploaders = emptySet()
        ) shouldBe false
    }

    @Test
    fun `filterByScanlator match all returns true only if all scanlators are filtered`() {
        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", "Group B"),
            uploader = "",
            all = true,
            filteredGroups = setOf("Group A", "Group B"),
            filteredUploaders = emptySet()
        ) shouldBe true

        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", "Group B"),
            uploader = "",
            all = true,
            filteredGroups = setOf("Group A"),
            filteredUploaders = emptySet()
        ) shouldBe false
    }

    @Test
    fun `filterByScanlator ignores merge source names`() {
        ChapterUtil.filterByScanlator(
            scanlators = listOf("Toonily", "Group A"),
            uploader = "",
            all = true,
            filteredGroups = setOf("Group A"),
            filteredUploaders = emptySet()
        ) shouldBe true
    }

    @Test
    fun `filterByScanlator handles uploader match when NO_GROUP is present under match any`() {
        ChapterUtil.filterByScanlator(
            scanlators = listOf(Constants.NO_GROUP),
            uploader = "Uploader X",
            all = false,
            filteredGroups = emptySet(),
            filteredUploaders = setOf("Uploader X")
        ) shouldBe true

        ChapterUtil.filterByScanlator(
            scanlators = listOf(Constants.NO_GROUP),
            uploader = "Uploader X",
            all = false,
            filteredGroups = emptySet(),
            filteredUploaders = setOf("Uploader Y")
        ) shouldBe false
    }

    @Test
    fun `filterByScanlator handles uploader match when NO_GROUP is present under match all`() {
        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", Constants.NO_GROUP),
            uploader = "Uploader X",
            all = true,
            filteredGroups = setOf("Group A"),
            filteredUploaders = setOf("Uploader X")
        ) shouldBe true

        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", Constants.NO_GROUP),
            uploader = "Uploader X",
            all = true,
            filteredGroups = setOf("Group A"),
            filteredUploaders = setOf("Uploader Y")
        ) shouldBe false

        // If NO_GROUP is in filteredGroups, it should not skip NO_GROUP
        ChapterUtil.filterByScanlator(
            scanlators = listOf("Group A", Constants.NO_GROUP),
            uploader = "Uploader X",
            all = true,
            filteredGroups = setOf("Group A", Constants.NO_GROUP),
            filteredUploaders = setOf("Uploader X")
        ) shouldBe true
    }
}
