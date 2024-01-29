package org.nekomanga.domain.manga

import org.nekomanga.R
import org.nekomanga.constants.MdConstants

enum class MangaDemographic(val key: String, val nameRes: Int) {
    None(MdConstants.Demographic.none, R.string.none),
    Shounen(MdConstants.Demographic.shounen, R.string.shounen),
    Shoujo(MdConstants.Demographic.shoujo, R.string.shoujo),
    Seinen(MdConstants.Demographic.seinen, R.string.seinen),
    Josei(MdConstants.Demographic.josei, R.string.josei),
    ;

    companion object {
        fun getOrdered(): List<MangaDemographic> {
            return listOf(None, Shounen, Shoujo, Seinen, Josei)
        }
    }
}
