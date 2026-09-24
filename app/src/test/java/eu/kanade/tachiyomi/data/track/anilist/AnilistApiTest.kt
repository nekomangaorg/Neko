package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.RecordingInterceptor
import eu.kanade.tachiyomi.data.track.TrackManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class AnilistApiTest {

    private val recorder =
        RecordingInterceptor("""{"data":{"SaveMediaListEntry":{"id":42,"status":"CURRENT"}}}""")

    @Before
    fun setup() {
        Injekt = InjektScope(DefaultRegistrar())
        Injekt.addSingleton(Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        Injekt = InjektScope(DefaultRegistrar())
    }

    private fun api() = AnilistApi(OkHttpClient.Builder().addInterceptor(recorder).build(), mockk())

    private fun track(score: Float) =
        Track.create(TrackManager.ANILIST).apply {
            media_id = 1L
            status = Anilist.READING
            this.score = score
        }

    private fun sentPayload(): JsonObject =
        Json.parseToJsonElement(recorder.requests.single().body!!).jsonObject

    @Test
    fun `addLibManga sends the score as scoreRaw`() = runTest {
        val track = track(score = 80f)

        api().addLibManga(track)

        val payload = sentPayload()
        payload["variables"]?.jsonObject?.get("score")?.jsonPrimitive?.intOrNull shouldBe 80
        val query = payload["query"]?.jsonPrimitive?.content.orEmpty()
        query shouldContain "${'$'}score: Int"
        query shouldContain "scoreRaw: ${'$'}score"
        track.library_id shouldBe 42L
    }

    @Test
    fun `addLibManga leaves the score variable out when the score is 0`() = runTest {
        api().addLibManga(track(score = 0f))

        sentPayload()["variables"]?.jsonObject?.containsKey("score") shouldBe false
    }
}
