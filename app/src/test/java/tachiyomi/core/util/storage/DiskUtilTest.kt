package tachiyomi.core.util.storage

import android.net.Uri
import android.os.StatFs
import com.hippo.unifile.UniFile
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DiskUtilTest {

    private val treeUriPath =
        "/tree/primary:Documents/document/primary:Documents/Neko/downloads/Manga"
    private val filePath = "/storage/emulated/0/Documents/Neko/downloads/Manga"

    @Before
    fun setup() {
        mockkConstructor(StatFs::class)
        every { constructedWith<StatFs>(EqMatcher(filePath)).availableBlocksLong } returns 10135L
        every { constructedWith<StatFs>(EqMatcher(filePath)).blockSizeLong } returns 4096L
        every { constructedWith<StatFs>(EqMatcher(filePath)).blockCountLong } returns 1500000L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun safFolder(resolvedPath: String?): UniFile {
        val uri = mockk<Uri> { every { path } returns treeUriPath }
        return mockk {
            every { this@mockk.uri } returns uri
            every { this@mockk.filePath } returns resolvedPath
        }
    }

    @Test
    fun `available space of a SAF folder is read from its file path`() {
        assertEquals(10135L * 4096L, DiskUtil.getAvailableStorageSpace(safFolder(filePath)))
    }

    @Test
    fun `available space is unknown when the folder has no file path`() {
        assertEquals(-1L, DiskUtil.getAvailableStorageSpace(safFolder(null)))
    }

    @Test
    fun `total space of a SAF folder is read from its file path`() {
        assertEquals(1500000L * 4096L, DiskUtil.getTotalStorageSpace(safFolder(filePath)))
    }
}
