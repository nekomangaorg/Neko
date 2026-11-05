package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.source.SourceManager
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class BackupFileValidatorTest {

    private val context = mockk<Context>(relaxed = true)
    private val uri = mockk<Uri>()
    private val sourceManager = mockk<SourceManager>()

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        every { sourceManager.isMangadex(any()) } answers { callOriginal() }
        every { sourceManager.isMangadexOrUnset(any()) } answers { callOriginal() }
        Injekt.addSingleton(sourceManager)
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    @Test
    fun `backup holding only rows with source -1 does not warn about missing MangaDex manga`() {
        assertFalse(validate(row(source = -1L)).missingMangaDexEntries)
    }

    @Test
    fun `backup holding only another source warns about missing MangaDex manga`() {
        assertTrue(validate(row(source = 12_345L)).missingMangaDexEntries)
    }

    private fun validate(vararg rows: BackupManga): BackupFileValidator.Results {
        val creator = BackupCreator(context)
        val encoded = creator.parser.encodeToByteArray(Backup.serializer(), Backup(rows.toList()))
        val gzipped = ByteArrayOutputStream()
        GZIPOutputStream(gzipped).use { it.write(encoded) }
        every { context.contentResolver.openInputStream(uri) } returns
            ByteArrayInputStream(gzipped.toByteArray())

        return BackupFileValidator(mockk(relaxed = true)).validate(context, uri)
    }

    private fun row(source: Long) =
        BackupManga(source = source, url = "/manga/00000000-0000-4000-8000-000000000001")
}
