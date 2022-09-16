package org.nekomanga.domain.manga

import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.lang.capitalizeWords

enum class MangaContentRating(val key: String) {
    Safe(MdConstants.ContentRating.safe),
    Suggestive(MdConstants.ContentRating.suggestive),
    Erotica(MdConstants.ContentRating.erotica),
    Pornographic(MdConstants.ContentRating.pornographic),
    Unknown(MdConstants.ContentRating.unknown),
    ;

    fun prettyPrint(): String {
        return key.capitalizeWords()
    }

    companion object {
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
