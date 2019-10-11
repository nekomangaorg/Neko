package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.english.Mangadex

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

    fun getNullableSource(sourceKey: Long): Source? {
        return source
    }


    fun getSources(): List<Source> {
        return listOf(source)
    }

    fun getMangadex() = source
}
