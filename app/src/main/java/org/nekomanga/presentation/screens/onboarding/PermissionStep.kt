package org.nekomanga.presentation.screens.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

internal class PermissionStep : OnboardingStep {

    private var installGranted by mutableStateOf(false)

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var installGranted by mutableStateOf(false)

        var notificationGranted by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                } else {
                    true
                })
        }

        var batteryGranted by remember {
            mutableStateOf(
                context
                    .getSystemService<PowerManager>()!!
                    .isIgnoringBatteryOptimizations(context.packageName))
        }

        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    installGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.packageManager.canRequestPackageInstalls()
                        } else {
                            @Suppress("DEPRECATION")
                            Settings.Secure.getInt(
                                context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) !=
                                0
                        }
                    batteryGranted =
                        context
                            .getSystemService<PowerManager>()!!
                            .isIgnoringBatteryOptimizations(context.packageName)
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Column(
            modifier = Modifier.padding(vertical = Size.medium),
        ) {
            PermissionItem(
                title = stringResource(R.string.onboarding_permission_install_apps),
                subtitle = stringResource(R.string.onboarding_permission_install_apps_description),
                granted = installGranted,
                onButtonClick = { context.launchRequestPackageInstallsPermission() },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { bool -> notificationGranted = bool },
                    )
                PermissionItem(
                    title = stringResource(R.string.onboarding_permission_notifications),
                    subtitle =
                        stringResource(R.string.onboarding_permission_notifications_description),
                    granted = notificationGranted,
                    onButtonClick = {
                        permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            PermissionItem(
                title = stringResource(R.string.onboarding_permission_ignore_battery_opts),
                subtitle =
                    stringResource(R.string.onboarding_permission_ignore_battery_opts_description),
                granted = batteryGranted,
                onButtonClick = {
                    @SuppressLint("BatteryLife")
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    context.startActivity(intent)
                },
            )
        }
    }

    @Composable
    private fun PermissionItem(
        title: String,
        subtitle: String,
        granted: Boolean,
        modifier: Modifier = Modifier,
        onButtonClick: () -> Unit,
    ) {
        ListItem(
            modifier = modifier,
            headlineContent = { Text(text = title) },
            supportingContent = { Text(text = subtitle) },
            trailingContent = {
                OutlinedButton(
                    enabled = !granted,
                    onClick = onButtonClick,
                ) {
                    if (granted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(stringResource(R.string.onboarding_permission_action_grant))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
