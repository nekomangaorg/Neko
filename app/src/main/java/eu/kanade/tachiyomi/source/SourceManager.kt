package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.Mangadex

/**
 *Currently hardcoded to always return the same English [Mangadex] instance
 */
open class SourceManager {

    //private val sourcesMap = mutableMapOf<Long, Source>()
    private val source: Source

    init {
        source = Mangadex("en", "gb", 1)
    }

    open fun get(sourceKey: Long): Source? {
        return source
    }

    fun getSource(sourceKey: Long): Source {
        return source
    }

    fun isMangadex(sourceKey: Long): Boolean {
        return possibleIds.contains(sourceKey)
    }


    fun getNullableSource(sourceKey: Long): Source? {
        return source
    }


    fun getSources(): List<Source> {
        return listOf(source)
    }

    fun getMangadex() = source

    companion object {
        val possibleIds = listOf(
                Mangadex("en", "gb", 1).id,
                Mangadex("ja", "jp", 2).id,
                Mangadex("pl", "pl", 3).id,
                Mangadex("sh", "rs", 4).id,
                Mangadex("nl", "nl", 5).id,
                Mangadex("it", "it", 6).id,
                Mangadex("ru", "ru", 7).id,
                Mangadex("de", "de", 8).id,
                Mangadex("hu", "hu", 9).id,
                Mangadex("fr", "fr", 10).id,
                Mangadex("fi", "fi", 11).id,
                Mangadex("vi", "vn", 12).id,
                Mangadex("el", "gr", 13).id,
                Mangadex("bg", "bg", 14).id,
                Mangadex("es", "es", 15).id,
                Mangadex("pt-BR", "br", 16).id,
                Mangadex("pt", "pt", 17).id,
                Mangadex("sv", "se", 18).id,
                Mangadex("ar", "sa", 19).id,
                Mangadex("da", "dk", 20).id,
                Mangadex("zh-Hans", "cn", 21).id,
                Mangadex("bn", "bd", 22).id,
                Mangadex("ro", "ro", 23).id,
                Mangadex("cs", "cz", 24).id,
                Mangadex("mn", "mn", 25).id,
                Mangadex("tr", "tr", 26).id,
                Mangadex("id", "id", 27).id,
                Mangadex("ko", "kr", 28).id,
                Mangadex("es-419", "mx", 29).id,
                Mangadex("fa", "ir", 30).id,
                Mangadex("ms", "my", 31).id,
                Mangadex("th", "th", 32).id,
                Mangadex("ca", "ct", 33).id,
                Mangadex("fil", "ph", 34).id,
                Mangadex("zh-Hant", "hk", 35).id,
                Mangadex("uk", "ua", 36),
                Mangadex("my", "mm", 37).id,
                Mangadex("lt", "lt", 38).id
        )
    }
}

