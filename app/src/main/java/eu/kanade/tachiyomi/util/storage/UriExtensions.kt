package eu.kanade.tachiyomi.util.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile

/**
 * Returns a uri that can be handed to other apps. A content uri (for example a document inside the
 * user's storage folder) already has an authority, so it is returned as is; a file uri is wrapped
 * by the app's [androidx.core.content.FileProvider].
 */
fun Uri.getUriWithAuthority(context: Context): Uri {
    if (scheme == ContentResolver.SCHEME_CONTENT) return this
    return this.toFile().getUriCompat(context)
}
