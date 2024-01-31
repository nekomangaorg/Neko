package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile

fun Uri.getUriWithAuthority(context: Context): Uri {
    return this.toFile().getUriCompat(context)
}
