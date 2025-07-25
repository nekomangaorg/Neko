package org.nekomanga.presentation.screens.onboarding

import android.content.ActivityNotFoundException
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import org.nekomanga.R
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.storage.storageLocationPicker
import org.nekomanga.presentation.components.storage.storageLocationText
import org.nekomanga.presentation.theme.Size
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
