package eu.kanade.tachiyomi.source.online

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMergedChapterOfType
import eu.kanade.tachiyomi.util.lang.toDisplayMessage
import kotlin.coroutines.cancellation.CancellationException
import okhttp3.Response
import org.nekomanga.R
import org.nekomanga.data.database.repository.ChapterRepository
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
 * Fetches a merged source's chapters and returns an empty list as an error when the manga has
 * stored chapters from that source. `syncChaptersWithSource` deletes the stored chapters of a
 * merged source that are missing from the fetched list unless `preserveMissingMergedChapters` is
 * set, so an empty list would delete all of them and their read state. With none stored the empty
 * list passes through, so merging a source that has no chapters for the series reports nothing.
 * Stored chapters carry no merge URL, so two merges of the same type share this check.
 */
suspend fun ReducedHttpSource.fetchMergedChapters(
    mergeManga: MergeMangaImpl,
    chapterRepository: ChapterRepository,
): Result<List<SChapterStatusPair>, ResultError> = runCatching {
    fetchChapters(mergeManga.url)
}
    .getOrElse { e ->
        if (e is CancellationException) throw e
        TimberKt.e(e) { "Failed to fetch chapters for merged source: $name" }
        Err(ResultError.Generic(e.toDisplayMessage()))
    }
    .andThen { chapters ->
        val wouldDeleteStoredChapters =
            chapters.isEmpty() &&
                chapterRepository.getChaptersForManga(mergeManga.mangaId).any {
                    it.isMergedChapterOfType(mergeManga.mergeType)
                }
        if (wouldDeleteStoredChapters) {
            Err(ResultError.Generic(errorRes = R.string.no_chapters_error))
        } else {
            Ok(chapters)
        }
    }
