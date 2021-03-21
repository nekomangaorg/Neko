package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.isomorphism.util.TokenBuckets
import rx.Observable
import java.util.concurrent.TimeUnit

abstract class ReducedHttpSource : HttpSource() {

    private val bucket = TokenBuckets.builder().withCapacity(1)
        .withFixedIntervalRefillStrategy(1, 10, TimeUnit.SECONDS).build()

    private val rateLimitInterceptor = Interceptor {
        bucket.consume()
        it.proceed(it.request())
    }

    override val client: OkHttpClient = network.cloudFlareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    override val headers = Headers.Builder()
        .add("Referer", "https://manga4life.com/")
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36").build()

    override fun fetchImage(page: Page): Observable<Response> {
        return client.newCallWithProgress(GET(page.imageUrl!!, headers), page)
            .asObservableSuccess()
    }

    override fun isLogged(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun login(username: String, password: String, twoFactorCode: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): Logout {
        TODO("Not yet implemented")
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override fun fetchFollows(): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAllFollows(forceHd: Boolean): List<SManga> {
        TODO("Not yet implemented")
    }

    override suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun fetchTrackingInfo(url: String): Track {
        TODO("Not yet implemented")
    }

    override fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        TODO("Not yet implemented")
    }

    override fun fetchMangaSimilarObservable(manga: Manga): Observable<MangasPage> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        TODO("Not yet implemented")
    }

    override fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        TODO("Not yet implemented")
    }
}
