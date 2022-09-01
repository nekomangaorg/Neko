package org.nekomanga.domain.manga

import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.lang.capitalizeWords

enum class MangaContentRating(val key: String, val color: Long) {
    Safe(MdConstants.ContentRating.safe, 0xFFF17575),
    Suggestive(MdConstants.ContentRating.suggestive, 0xFF3BAEEA),
    Erotica(MdConstants.ContentRating.erotica, 0xFF7BD555),
    Pornographic(MdConstants.ContentRating.pornographic, 0xFFff00ff),
    Unknown(MdConstants.ContentRating.unknown, 0xFF45818e);

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
