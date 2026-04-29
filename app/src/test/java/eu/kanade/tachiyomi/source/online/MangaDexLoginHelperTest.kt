package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper.Companion.classifyHttpFailure
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper.RefreshOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import tachiyomi.core.network.HttpException

class MangaDexLoginHelperTest {

    @Test
    fun `HTTP 400 classifies as persistent`() {
        val outcome = classifyHttpFailure(400)

        outcome.shouldBeInstanceOf<RefreshOutcome.Persistent>()
        (outcome.cause as HttpException).code shouldBe 400
    }

    @Test
    fun `HTTP 401 classifies as persistent`() {
        val outcome = classifyHttpFailure(401)

        outcome.shouldBeInstanceOf<RefreshOutcome.Persistent>()
    }

    @Test
    fun `HTTP 403 classifies as persistent`() {
        val outcome = classifyHttpFailure(403)

        outcome.shouldBeInstanceOf<RefreshOutcome.Persistent>()
    }

    @Test
    fun `HTTP 500 classifies as transient`() {
        val outcome = classifyHttpFailure(500)

        outcome.shouldBeInstanceOf<RefreshOutcome.Transient>()
        (outcome.cause as HttpException).code shouldBe 500
    }

    @Test
    fun `HTTP 502 classifies as transient`() {
        val outcome = classifyHttpFailure(502)

        outcome.shouldBeInstanceOf<RefreshOutcome.Transient>()
    }

    @Test
    fun `HTTP 503 classifies as transient`() {
        val outcome = classifyHttpFailure(503)

        outcome.shouldBeInstanceOf<RefreshOutcome.Transient>()
    }

    @Test
    fun `HTTP 599 classifies as transient`() {
        val outcome = classifyHttpFailure(599)

        outcome.shouldBeInstanceOf<RefreshOutcome.Transient>()
    }

    @Test
    fun `HTTP 429 classifies as transient`() {
        val outcome = classifyHttpFailure(429)

        outcome.shouldBeInstanceOf<RefreshOutcome.Transient>()
        (outcome.cause as HttpException).code shouldBe 429
    }

    @Test
    fun `HTTP 600 classifies as persistent`() {
        // Out of the 5xx range; treat as protocol error.
        val outcome = classifyHttpFailure(600)

        outcome.shouldBeInstanceOf<RefreshOutcome.Persistent>()
    }
}
