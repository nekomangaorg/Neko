package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import org.nekomanga.BuildConfig

/**
 * Returns the uri of a file
 *
 * @param context context of application
 */
fun File.getUriCompat(context: Context): Uri {
    return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", this)
}
