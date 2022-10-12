package org.nekomanga.domain.manga

import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.lang.capitalizeWords

enum class MangaDemographic(val key: String) {
    None(MdConstants.Demographic.none),
    Shounen(MdConstants.Demographic.shounen),
    Shoujo(MdConstants.Demographic.shoujo),
    Seinen(MdConstants.Demographic.seinen),
    Josei(MdConstants.Demographic.josei)
    ;

    fun prettyPrint(): String {
        return key.capitalizeWords()
    }

    companion object {
        fun getOrdered(): List<MangaDemographic> {
            return listOf(None, Shounen, Shoujo, Seinen, Josei)
        }
    }
}
