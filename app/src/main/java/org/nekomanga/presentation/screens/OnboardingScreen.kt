package org.nekomanga.presentation.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.kanade.tachiyomi.ui.main.ObserveAsEvents
import eu.kanade.tachiyomi.util.system.toast
import org.nekomanga.R
import org.nekomanga.presentation.InfoScreen
import org.nekomanga.presentation.components.storage.storageLocationPicker
import org.nekomanga.presentation.screens.onboarding.FirstStepContent
import org.nekomanga.presentation.screens.onboarding.OnboardingEvent
import org.nekomanga.presentation.screens.onboarding.OnboardingViewModel
import org.nekomanga.presentation.screens.onboarding.PermissionStep
import org.nekomanga.presentation.screens.onboarding.StorageStepContent
import org.nekomanga.presentation.screens.onboarding.ThemeStepContent
import org.nekomanga.presentation.theme.Size
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

private const val TotalOnboardingSteps = 4
private const val StorageStepIndex = 2

@Composable
fun OnboardingScreen(
    finishedOnBoarding: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val slideDistance = rememberSlideDistance()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val handler = LocalUriHandler.current

    ObserveAsEvents(flow = viewModel.eventChannel) { event ->
        when (event) {
            is OnboardingEvent.RecreateActivity -> {
                (context as? Activity)?.let { activity -> ActivityCompat.recreate(activity) }
            }
        }
    }

    val pickStorageLocation = storageLocationPicker(viewModel.baseStorageDirectory)
    val permissionStep = remember { PermissionStep() }
    val isLastStep = currentStep == TotalOnboardingSteps - 1
    val canAccept =
        if (currentStep == StorageStepIndex) {
            uiState.storageStepUiState.isStorageConfigured
        } else {
            true
        }

    BackHandler(
        enabled = true,
        onBack = {
            if (currentStep > 0) {
                currentStep--
            }
        },
    )

    InfoScreen(
        headingText = stringResource(R.string.onboarding_heading),
        subtitleText = stringResource(R.string.onboarding_description),
        acceptText =
            stringResource(
                when (isLastStep) {
                    true -> R.string.onboarding_action_finish
                    false -> R.string.onboarding_action_next
                },
            ),
        tint = MaterialTheme.colorScheme.primary,
        canAccept = canAccept,
        onAcceptClick = {
            when (isLastStep) {
                true -> {
                    finishedOnBoarding()
                }
                false -> currentStep++
            }
        },
    ) {
        Box(
            modifier =
                Modifier.padding(vertical = Size.small)
                    .clip(MaterialTheme.shapes.small)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                label = "stepContent",
            ) { step ->
                when (step) {
                    0 -> FirstStepContent()
                    1 ->
                        ThemeStepContent(
                            uiState = uiState.themeStepUiState,
                            onThemeSelected = viewModel::onThemeSelected,
                            onFollowSystemToggle = viewModel::onFollowSystemToggle,
                        )
                    2 ->
                        StorageStepContent(
                            uiState = uiState.storageStepUiState,
                            onSelectStorage = {
                                try {
                                    pickStorageLocation.launch(null)
                                } catch (e: ActivityNotFoundException) {
                                    context.toast(R.string.file_picker_error)
                                }
                            },
                            onOpenHelp = {
                                handler.openUri(
                                    "https://mihon.app/docs/faq/storage#migrating-from-tachiyomi-v0-14-x-or-earlier",
                                )
                            },
                        )
                    3 -> permissionStep.Content()
                }
            }
        }
    }
}
