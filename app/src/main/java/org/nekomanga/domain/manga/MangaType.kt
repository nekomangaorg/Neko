package org.nekomanga.domain.manga

import androidx.annotation.StringRes
import org.nekomanga.R

enum class MangaType(@StringRes val typeRes: Int) {
    Manhua(R.string.manhua),
    Manhwa(R.string.manhwa),
    Manga(R.string.manga),
    Unknown(R.string.unknown);

    companion object {
        fun fromLangFlag(lang: String?): MangaType {
            return when (lang) {
                "ko" -> Manhwa
                "zh" -> Manhua
                "zh-hk" -> Manhua
                null -> Unknown
                else -> Manga
            }
        }
    }
}
