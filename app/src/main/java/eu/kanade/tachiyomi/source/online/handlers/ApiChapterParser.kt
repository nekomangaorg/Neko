package eu.kanade.tachiyomi.source.online.handlers

import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.handlers.serializers.ApiChapterSerializer
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.Response
import java.util.Date

class ApiChapterParser {
    fun pageListParse(response: Response): List<Page> {
        val jsonData = response.body!!.string()
        val networkApiChapter = MdUtil.jsonParser.decodeFromString(ApiChapterSerializer.serializer(), jsonData)

        val pages = mutableListOf<Page>()

        val hash = networkApiChapter.data.hash
        val pageArray = networkApiChapter.data.pages
        val server = networkApiChapter.data.server

        pageArray.forEach {
            val url = "$hash/${it}"
            pages.add(Page(pages.size, "$server,${response.request.url},${Date().time}", url))
        }

        return pages
    }

    fun externalParse(response: Response): String {
        val jsonData = response.body!!.string()
        val json = JsonParser.parseString(jsonData).asJsonObject
        val external = json.get("data").get("pages").string
        return external.substringAfterLast("/")
    }
}
