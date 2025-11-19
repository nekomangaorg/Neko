package eu.kanade.tachiyomi.source.online.merged

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.SChapterStatusPair
import okhttp3.Headers
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.network.ResultError

class InvalidHttpSource: ReducedHttpSource() {
    override suspend fun searchManga(query: String): List<SManga> {
        return emptyList()
    }

    override suspend fun fetchChapters(mangaUrl: String): Result<List<SChapterStatusPair>, ResultError> {
        return Err(ResultError.Generic("Invalid merge source unmerge"))
    }

    override val headers: Headers
        get() = TODO("Not yet implemented")

    override fun getChapterUrl(simpleChapter: SimpleChapter): String {
        return ""
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return emptyList()
    }

}
