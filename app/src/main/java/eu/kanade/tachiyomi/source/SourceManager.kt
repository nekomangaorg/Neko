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
        //sourcesMap[source.id] = source
    }

    open fun get(sourceKey: Long): Source? {
        //return sourcesMap[sourceKey]
        return source
    }

    fun getSource(sourceKey: Long): Source {
        //return sourcesMap[sourceKey]!!
        return source
    }

    fun getNullableSource(sourceKey: Long): Source? {
        //return sourcesMap[sourceKey]
        return source
    }


    fun getSources(): List<Source> {
        //return sourcesMap.values.toList()
        return listOf(source)
    }

    fun getMangadex() = source
}
