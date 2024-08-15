package org.nekomanga.presentation.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

class FirstStep : OnboardingStep {
    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        Column(
            modifier = Modifier.padding(vertical = Size.medium),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text =
                    stringResource(
                        R.string.onboarding_guides_returning_user,
                        stringResource(R.string.app_name)),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center)
            Gap(Size.medium)

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_guides_new_version),
                textAlign = TextAlign.Center)

            Gap(Size.medium)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_guides_restore_backup),
                textAlign = TextAlign.Center)
            Text(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                text = stringResource(R.string.onboarding_guides_restore_backup_setting_location),
                textAlign = TextAlign.Center)
        }
    }
}
