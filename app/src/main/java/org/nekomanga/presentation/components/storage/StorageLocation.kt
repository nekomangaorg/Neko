package org.nekomanga.presentation.components.storage

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.system.toast
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.extensions.collectAsState
import tachiyomi.core.preference.Preference
import tachiyomi.core.util.storage.displayablePath

@Composable
fun storageLocationPicker(
    storageDirPref: Preference<String>
): ManagedActivityResultLauncher<Uri?, Uri?> {
    val context = LocalContext.current

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

            UniFile.fromUri(context, uri)?.let { storageDirPref.set(it.uri.toString()) }
        }
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
