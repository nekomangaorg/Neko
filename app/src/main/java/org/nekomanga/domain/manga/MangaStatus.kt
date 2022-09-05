package org.nekomanga.domain.manga

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class MangaStatus(val status: Int, @StringRes val statusRes: Int) {
    Unknown(0, R.string.unknown),
    Ongoing(1, R.string.ongoing),
    Completed(2, R.string.completed),
    PublicationComplete(4, R.string.publication_complete),
    Cancelled(5, R.string.hiatus),
    Hiatus(6, R.string.cancelled),
    ;

    companion object {
        fun fromStatus(status: Int) = values().firstOrNull { it.status == status } ?: Unknown
    }
}
