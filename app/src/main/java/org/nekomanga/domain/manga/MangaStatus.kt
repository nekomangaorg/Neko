package org.nekomanga.domain.manga

import androidx.annotation.ColorLong
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class MangaStatus(val status: Int, @StringRes val statusRes: Int, @ColorLong val color: Long) {
    Unknown(0, R.string.unknown, 0xFF45818e),
    Ongoing(1, R.string.ongoing, 0xFF3BAEEA),
    Completed(2, R.string.completed, 0xFF7BD555),
    PublicationComplete(4, R.string.publication_complete, 0xFFd29d2f),
    Cancelled(5, R.string.hiatus, 0xFFF17575),
    Hiatus(6, R.string.cancelled, 0xFFff00ff),
    ;

    companion object {
        fun fromStatus(status: Int) = values().firstOrNull { it.status == status } ?: Unknown
    }
}
