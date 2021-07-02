package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MergeSource
import eu.kanade.tachiyomi.source.online.utils.MdLang
import uy.kohesive.injekt.injectLazy
import java.security.MessageDigest

/**
 *Currently hardcoded to always return the same English [MangaDex] instance
 */
open class SourceManager {

    private val preferences: PreferencesHelper by injectLazy()

    // private val sourcesMap = mutableMapOf<Long, Source>()
    private val source: MangaDex = MangaDex()

    private val mergeSource = MergeSource()

    open fun get(sourceKey: Long): Source? {
        return source
    }

    fun isMangadex(sourceKey: Long): Boolean {
        return possibleIds.contains(sourceKey)
    }

    fun getMangadex(): MangaDex = source

    fun getMergeSource() = mergeSource

    companion object {

        val possibleIds = MdLang.values().map { getId(it.lang) }

        fun getId(lang: String): Long {
            val key = "mangadex/$lang/1"
            val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }
                .reduce(Long::or) and Long.MAX_VALUE
        }
    }
}
