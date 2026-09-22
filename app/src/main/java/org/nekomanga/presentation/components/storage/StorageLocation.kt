package org.nekomanga.presentation.components.storage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.extensions.collectAsState
import tachiyomi.core.preference.Preference
import tachiyomi.core.util.storage.displayablePath

@Composable
fun storageLocationPicker(
    storageDirPref: Preference<String>
): ManagedActivityResultLauncher<Uri?, Uri?> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // For some reason InkBook devices do not implement the SAF properly. Persistable URI
            // grants do not
            // work. However, simply retrieving the URI and using it works fine for these devices.
            // Access is not
            // revoked after the app is closed or the device is restarted.
            // This also holds for some Samsung devices. Thus, we simply execute inside of a
            // try-catch block and
            // ignore the exception if it is thrown.
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                TimberKt.e(e)
                context.toast(R.string.file_picker_uri_permission_unsupported)
            }

            UniFile.fromUri(context, uri)?.let { directory ->
                scope.launchIO {
                    if (directory.canHostAppDirectories()) {
                        storageDirPref.set(directory.uri.toString())
                    } else {
                        withUIContext {
                            context.toast(
                                context.getString(
                                    R.string.invalid_location,
                                    directory.displayablePath,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Some document providers hand out a tree the app cannot create folders in, the Downloads shortcut
 * of a few file managers among them. Saving such a location leaves every download failing with an
 * invalid location error, so probe it with the folder the app needs first.
 */
internal fun UniFile.canHostAppDirectories(): Boolean {
    return try {
        createDirectory(StoragePreferences.DOWNLOADS_DIR) != null
    } catch (e: Exception) {
        TimberKt.e(e) { "Error creating a folder in $uri" }
        false
    }
}

@Composable
fun storageLocationText(storageDirPref: Preference<String>): String {
    val context = LocalContext.current
    val storageDir by storageDirPref.collectAsState()

    if (storageDir == storageDirPref.defaultValue()) {
        return stringResource(R.string.no_location_set)
    }

    return remember(storageDir) {
        val file = UniFile.fromUri(context, storageDir.toUri())
        file?.displayablePath
    } ?: stringResource(R.string.invalid_location, storageDir)
}
