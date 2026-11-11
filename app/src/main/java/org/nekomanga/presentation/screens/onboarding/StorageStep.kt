package org.nekomanga.presentation.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

internal class StorageStep(
    private val uiState: StorageStepUiState,
    private val onSelectStorage: () -> Unit,
    private val onOpenHelp: () -> Unit,
) : OnboardingStep {
    override val isComplete: Boolean
        get() = uiState.isStorageConfigured

    @Composable
    override fun Content() {
        StorageStepContent(
            uiState = uiState,
            onSelectStorage = onSelectStorage,
            onOpenHelp = onOpenHelp,
        )
    }
}

@Composable
fun StorageStepContent(
    uiState: StorageStepUiState,
    onSelectStorage: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Size.medium).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        Text(
            text =
                stringResource(
                    R.string.onboarding_storage_info,
                    stringResource(R.string.app_name),
                    uiState.currentStoragePath,
                ),
        )

        Button(modifier = Modifier.fillMaxWidth(), onClick = onSelectStorage) {
            Text(stringResource(R.string.onboarding_storage_action_select))
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Size.small),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        Text(
            text =
                stringResource(
                    R.string.onboarding_storage_help_info,
                    stringResource(R.string.app_name),
                ),
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenHelp) {
            Text(stringResource(R.string.onboarding_storage_help_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageStepContentConfiguredPreview() {
    StorageStepContent(
        uiState =
            StorageStepUiState(
                currentStoragePath = "/storage/emulated/0/Neko",
                isStorageConfigured = true,
            ),
        onSelectStorage = {},
        onOpenHelp = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun StorageStepContentNotConfiguredPreview() {
    StorageStepContent(
        uiState =
            StorageStepUiState(
                currentStoragePath = "No location set",
                isStorageConfigured = false,
            ),
        onSelectStorage = {},
        onOpenHelp = {},
    )
}
