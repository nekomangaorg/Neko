package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.RecordingInterceptor
import eu.kanade.tachiyomi.data.track.TrackManager
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Test

class KitsuApiTest {

    private val recorder = RecordingInterceptor("""{"data":{"id":"123"}}""")
    private val api = KitsuApi(OkHttpClient.Builder().addInterceptor(recorder).build(), mockk())

    private fun track(score: Float) =
        Track.create(TrackManager.KITSU).apply {
            media_id = 7L
            status = Kitsu.READING
            this.score = score
        }

    private fun sentAttributes(): JsonObject? =
        Json.parseToJsonElement(recorder.requests.single().body!!)
            .jsonObject["data"]
            ?.jsonObject
            ?.get("attributes")
            ?.jsonObject

    @Test
    fun `addLibManga sends the score as ratingTwenty`() = runTest {
        val track = track(score = 8f)

        api.addLibManga(track, "1")

        sentAttributes()?.get("ratingTwenty")?.jsonPrimitive?.content shouldBe "16"
        track.media_id shouldBe 123L
    }

    @Test
    fun `addLibManga leaves ratingTwenty out when the score is 0`() = runTest {
        api.addLibManga(track(score = 0f), "1")

        sentAttributes()?.containsKey("ratingTwenty") shouldBe false
    }
}
