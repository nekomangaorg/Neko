package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class PageTest {

    @Test
    fun `error message is kept while the page is in error`() {
        val page = Page(0)
        page.errorMessage = "HTTP 410 from atsu.moe"
        page.status = Page.State.ERROR

        page.errorMessage shouldBe "HTTP 410 from atsu.moe"
    }

    @Test
    fun `error message is cleared when the page leaves the error state`() {
        val page = Page(0)
        page.errorMessage = "HTTP 410 from atsu.moe"
        page.status = Page.State.ERROR

        page.status = Page.State.QUEUE

        page.errorMessage.shouldBeNull()
    }
}
