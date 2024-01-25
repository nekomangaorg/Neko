package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Response
import org.nekomanga.domain.network.ResultError
import tachiyomi.core.network.await
import tachiyomi.core.network.newCachelessCallWithProgress

abstract class ReducedHttpSource : HttpSource() {

    abstract suspend fun searchManga(query: String): List<SManga>

    abstract suspend fun fetchChapters(mangaUrl: String): Result<List<SChapter>, ResultError>

    override suspend fun getImage(page: Page): Response {
        return client.newCachelessCallWithProgress(imageRequest(page), page).await()
    }

    companion object {
        const val userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36"
    }
}
