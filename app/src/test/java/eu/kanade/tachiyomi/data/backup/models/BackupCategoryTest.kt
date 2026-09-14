package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.nekomanga.presentation.screens.library.LibrarySort

@OptIn(ExperimentalSerializationApi::class)
class BackupCategoryTest {

    private val parser = ProtoBuf

    @Test
    fun `copyFrom keeps the category sort`() {
        val category =
            CategoryImpl().apply {
                name = "Reading"
                order = 2
                flags = 4
                mangaSort = LibrarySort.LastRead.categoryValueDescending
            }

        val backup = BackupCategory.copyFrom(category)

        assertEquals("Reading", backup.name)
        assertEquals(2, backup.order)
        assertEquals(4, backup.flags)
        assertEquals(LibrarySort.LastRead.categoryValueDescending, backup.mangaSort)
    }

    @Test
    fun `sort survives a protobuf round trip`() {
        val backup =
            BackupCategory(
                name = "Reading",
                order = 1,
                mangaSort = LibrarySort.Unread.categoryValue,
            )

        val bytes = parser.encodeToByteArray(BackupCategory.serializer(), backup)
        val restored = parser.decodeFromByteArray(BackupCategory.serializer(), bytes)

        assertEquals(LibrarySort.Unread.categoryValue, restored.mangaSort)
        assertEquals(LibrarySort.Unread.categoryValue, restored.getCategoryImpl().mangaSort)
    }

    @Test
    fun `older backups without a sort restore with no sort`() {
        val legacy = BackupCategory(name = "Reading", order = 1)

        val bytes = parser.encodeToByteArray(BackupCategory.serializer(), legacy)
        val restored = parser.decodeFromByteArray(BackupCategory.serializer(), bytes)

        assertNull(restored.mangaSort)
        assertNull(restored.getCategoryImpl().mangaSort)
    }
}
