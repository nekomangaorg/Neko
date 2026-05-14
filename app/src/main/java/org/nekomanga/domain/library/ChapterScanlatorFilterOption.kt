package org.nekomanga.domain.library

enum class ChapterScanlatorFilterOption(val value: Int) {
    ALL(0),
    ANY(1);

    companion object {
        fun fromInt(value: Int): ChapterScanlatorFilterOption {
            return entries.firstOrNull { it.value == value } ?: ANY
        }
    }
}
