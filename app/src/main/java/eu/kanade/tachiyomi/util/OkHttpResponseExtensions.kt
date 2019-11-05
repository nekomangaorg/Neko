package eu.kanade.tachiyomi.util

import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

fun Response.consumeBody(): String? {
    use {
        if (it.code != 200) throw Exception("HTTP error ${it.code}")
        return it.body?.string()
    }
}

fun Response.consumeXmlBody(): String? {
    use { res ->
        if (res.code != 200) throw Exception("Export list error")
        BufferedReader(InputStreamReader(GZIPInputStream(res.body?.source()?.inputStream()))).use { reader ->
            val sb = StringBuilder()
            reader.forEachLine { line ->
                sb.append(line)
            }
            return sb.toString()
        }
    }
}