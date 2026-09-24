package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.RecordingInterceptor
import eu.kanade.tachiyomi.data.track.TrackManager
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Test

class MangaUpdatesApiTest {

    private val recorder = RecordingInterceptor()
    private val api =
        MangaUpdatesApi(mockk(), OkHttpClient.Builder().addInterceptor(recorder).build())

    private fun track(score: Float) =
        Track.create(TrackManager.MANGA_UPDATES).apply {
            media_id = 5L
            status = MangaUpdates.READING_LIST
            this.score = score
        }

    @Test
    fun `addSeriesToList sends the score as the series rating`() = runTest {
        api.addSeriesToList(track(score = 8f))

        recorder.requests.map { it.method to it.path } shouldBe
            listOf("POST" to "/v1/lists/series", "PUT" to "/v1/series/5/rating")
        Json.parseToJsonElement(recorder.requests[1].body!!)
            .jsonObject["rating"]
            ?.jsonPrimitive
            ?.floatOrNull shouldBe 8f
    }

    @Test
    fun `addSeriesToList leaves the series rating alone when the score is 0`() = runTest {
        api.addSeriesToList(track(score = 0f))

        recorder.requests.map { it.method to it.path } shouldBe listOf("POST" to "/v1/lists/series")
    }
}
