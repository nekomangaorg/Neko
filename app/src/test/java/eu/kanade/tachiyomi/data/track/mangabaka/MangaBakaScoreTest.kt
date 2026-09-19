package eu.kanade.tachiyomi.data.track.mangabaka

import io.kotest.matchers.shouldBe
import org.junit.Test

class MangaBakaScoreTest {

    @Test
    fun `fromTenPointScore scales to the 100 point storage`() {
        MangaBaka.fromTenPointScore(7f) shouldBe 70f
    }
}
