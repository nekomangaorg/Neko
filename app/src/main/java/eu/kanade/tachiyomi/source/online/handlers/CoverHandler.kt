package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.CoversResult
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.OkHttpClient

class CoverHandler(val client: OkHttpClient, val headers: Headers) {

    suspend fun getCovers(manga: SManga): List<String> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(GET("${MdUtil.baseUrl}${MdUtil.coversApi}${MdUtil.getMangaId(manga.url)}", headers, CacheControl.FORCE_NETWORK)).execute()
            val result = Json.nonstrict.parse(CoversResult.serializer(), response.body!!.string())
            result.covers.map { "${MdUtil.baseUrl}$it" }
        }
    }
}