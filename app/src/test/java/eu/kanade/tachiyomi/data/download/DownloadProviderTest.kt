package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import tachiyomi.core.util.storage.DiskUtil

class DownloadProviderTest {

    @Test
    fun `title-only naming collides for duplicate titles`() {
        val title = "Dragon Ball"
        DiskUtil.buildValidFilename(title) shouldBe DiskUtil.buildValidFilename(title)
    }

    @Test
    fun `uuid-based naming distinguishes manga with same title`() {
        val title = "Dragon Ball"
        val uuid1 = "a96676be-9d0c-4a78-9e0b-2d52b9e5cf72"
        val uuid2 = "b12345ab-1234-5678-abcd-ef1234567890"
        DiskUtil.buildValidFilename("$title [$uuid1]") shouldNotBe
            DiskUtil.buildValidFilename("$title [$uuid2]")
    }
}
