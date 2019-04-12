package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.online.english.Mangadex

open class SourceManager {

    private val sourcesMap = mutableMapOf<Long, Source>()

    init {
       val source =  Mangadex("en", "gb", 1)
        sourcesMap[source.id] = source
    }

    open fun get(sourceKey: Long): Source? {
        return sourcesMap[sourceKey]
    }
    fun getSource(sourceKey: Long): Source = sourcesMap[sourceKey]!!

    fun getSources() = sourcesMap.values.toList()

}
