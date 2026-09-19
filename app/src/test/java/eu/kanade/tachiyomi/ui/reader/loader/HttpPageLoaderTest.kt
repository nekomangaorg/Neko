package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoader.Companion.httpErrorMessage
import io.kotest.matchers.shouldBe
import org.junit.Test

class HttpPageLoaderTest {

    @Test
    fun `http error message names the status code and the host`() {
        httpErrorMessage(410, "atsu.moe") shouldBe "HTTP 410 from atsu.moe"
    }
}
