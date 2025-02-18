package org.nekomanga.presentation.screens.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import tachiyomi.core.preference.Preference
import tachiyomi.core.util.storage.displayablePath
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class StorageStep : OnboardingStep {

    private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val pickStorageLocation = storageLocationPicker(storagePref)

        Column(
            modifier = Modifier.padding(Size.medium).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Size.small),
        ) {
            Text(
                stringResource(
                    R.string.onboarding_storage_info,
                    stringResource(R.string.app_name),
                    storageLocationText(storagePref),
                )
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        pickStorageLocation.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        context.toast(R.string.file_picker_error)
                    }
                },
            ) {
                Text(stringResource(R.string.onboarding_storage_action_select))
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Size.small),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Text(
                stringResource(
                    R.string.onboarding_storage_help_info,
                    stringResource(R.string.app_name),
                )
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    handler.openUri(
                        "https://mihon.app/docs/faq/storage#migrating-from-tachiyomi-v0-14-x-or-earlier"
                    )
                },
            ) {
                Text(stringResource(R.string.onboarding_storage_help_action))
            }
        }

        LaunchedEffect(Unit) {
            storagePref.changes().collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}

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

            context.contentResolver.takePersistableUriPermission(uri, flags)

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
