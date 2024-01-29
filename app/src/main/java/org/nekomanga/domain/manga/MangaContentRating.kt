package org.nekomanga.domain.manga

import eu.kanade.tachiyomi.util.lang.capitalizeWords
import org.nekomanga.R
import org.nekomanga.constants.MdConstants

enum class MangaContentRating(val key: String, val nameRes: Int) {
    Safe(MdConstants.ContentRating.safe, R.string.safe),
    Suggestive(MdConstants.ContentRating.suggestive, R.string.suggestive),
    Erotica(MdConstants.ContentRating.erotica, R.string.erotica),
    Pornographic(MdConstants.ContentRating.pornographic, R.string.pornographic),
    Unknown(MdConstants.ContentRating.unknown, R.string.unknown),
    ;

    fun prettyPrint(): String {
        return key.capitalizeWords()
    }

    companion object {
        fun getOrdered(): List<MangaContentRating> {
            return listOf(Safe, Suggestive, Erotica, Pornographic)
        }

        fun getContentRating(contentRating: String?): MangaContentRating {
            return when {
                contentRating == null -> Unknown
                contentRating.equals(Safe.key, true) -> Safe
                contentRating.equals(Suggestive.key, true) -> Suggestive
                contentRating.equals(Erotica.key, true) -> Erotica
                contentRating.equals(Pornographic.key, true) -> Pornographic
                else -> Safe
            }
        }
    }
}
