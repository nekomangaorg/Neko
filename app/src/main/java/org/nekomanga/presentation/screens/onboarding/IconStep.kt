package org.nekomanga.presentation.screens.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.nekomanga.domain.nekoAppIcons
import org.nekomanga.presentation.components.LauncherIcon
import org.nekomanga.presentation.theme.Size

class IconStep : OnboardingStep {
    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        Column(
            modifier = Modifier.padding(vertical = Size.medium),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Size.small)
            ) {
                nekoAppIcons.forEach {
                    LauncherIcon(size = Size.extraHuge, iconId = it.icon) {
                        setIcon(context, it.component)
                    }
                }
            }
        }
    }
}

private fun setIcon(
    context: Context,
    componentName: String,
) {
    val packageManager = context.packageManager

    packageManager.setComponentEnabledSetting(
        ComponentName(context, componentName),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )

    nekoAppIcons
        .filter { it.component != componentName }
        .forEach {
            packageManager.setComponentEnabledSetting(
                ComponentName(context, it.component),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
}
