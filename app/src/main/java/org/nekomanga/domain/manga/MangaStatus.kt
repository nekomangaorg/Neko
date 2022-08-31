package org.nekomanga.domain.manga

import androidx.annotation.ColorLong
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class MangaStatus(val status: Int, @StringRes val statusRes: Int, @ColorLong val color: Long) {
    Unknown(0, R.string.unknown, 0x775b5b5b),
    Ongoing(1, R.string.ongoing, 0xFF3BAEEA),
    Completed(2, R.string.completed, 0x777BD555),
    Licensed(3, R.string.licensed, 0x77F79A63),
    PublicationComplete(4, R.string.publication_complete, 0x77d29d2f),
    Cancelled(5, R.string.hiatus, 0x77F17575),
    Hiatus(6, R.string.cancelled, 0x77E85D75),
    ;

    companion object {
        fun fromStatus(status: Int) = values().firstOrNull { it.status == status } ?: Unknown
    }
}
