package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.toDisplayMessage
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.Response
import org.nekomanga.R
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import tachiyomi.core.network.newCachelessCallWithProgress

typealias SChapterStatusPair = Pair<SChapter, Boolean>

abstract class ReducedHttpSource : HttpSource() {

    abstract suspend fun searchManga(query: String): List<SManga>

    open fun getMangaUrl(url: String): String = baseUrl + url

    abstract suspend fun fetchChapters(
        mangaUrl: String
    ): Result<List<SChapterStatusPair>, ResultError>

    override suspend fun getImage(page: Page): Response {
        return client.newCachelessCallWithProgress(imageRequest(page), page).await()
    }

    companion object {
        const val userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36"
    }
}

/**
 * Fetches a merged source's chapters and returns an empty list as an error.
 * `syncChaptersWithSource` deletes the stored chapters of a merged source that are missing from the
 * fetched list unless `errorFromMerged` is set, so an empty list would delete all of them and their
 * read state.
 */
suspend fun ReducedHttpSource.fetchMergedChapters(
    mangaUrl: String
): Result<List<SChapterStatusPair>, ResultError> = runCatching {
    fetchChapters(mangaUrl)
}
    .getOrElse { e ->
        if (e is CancellationException) throw e
        TimberKt.e(e) { "Failed to fetch chapters for merged source: $name" }
        Err(ResultError.Generic(e.toDisplayMessage()))
    }
    .andThen { chapters ->
        if (chapters.isEmpty()) Err(ResultError.Generic(errorRes = R.string.no_chapters_error))
        else Ok(chapters)
    }
