package eu.kanade.tachiyomi.data.track.anilist

import io.kotest.matchers.shouldBe
import org.junit.Test

class AnilistScoreTest {

    @Test
    fun `fromTenPointScore scales to the 100 point storage`() {
        Anilist.fromTenPointScore(8f) shouldBe 80f
        Anilist.fromTenPointScore(0f) shouldBe 0f
    }
}
