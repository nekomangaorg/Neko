package eu.kanade.tachiyomi.source.online.handlers

import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import okhttp3.Response

class ApiChapterParser {
    fun pageListParse(response: Response): List<Page> {
        val jsonData = response.body!!.string()
        val json = JsonParser().parse(jsonData).asJsonObject

        val pages = mutableListOf<Page>()

        val hash = json.get("hash").string
        val pageArray = json.getAsJsonArray("page_array")
        val server = json.get("server").string

        pageArray.forEach {
            val url = "$server$hash/${it.asString}"
            pages.add(Page(pages.size, "", MdUtil.getImageUrl(url)))
        }

        return pages
    }
}