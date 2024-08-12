package eu.kanade.tachiyomi.ui.onboarding

import android.annotation.SuppressLint
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
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BasicComposeController
import org.nekomanga.R
import org.nekomanga.presentation.InfoScreen
import org.nekomanga.presentation.screens.onboarding.FirstStep
import org.nekomanga.presentation.screens.onboarding.PermissionStep
import org.nekomanga.presentation.screens.onboarding.StorageStep
import org.nekomanga.presentation.screens.onboarding.ThemeStep
import org.nekomanga.presentation.theme.Size
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OnboardingController : BasicComposeController() {

    val preferences: PreferencesHelper = Injekt.get()

    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @Composable
    override fun ScreenContent() {

        val slideDistance = rememberSlideDistance()

        var currentStep by rememberSaveable { mutableIntStateOf(0) }

        val steps = remember { listOf(FirstStep(), ThemeStep(), StorageStep(), PermissionStep()) }

        val isLastStep = currentStep == steps.lastIndex

        BackHandler(
            enabled = true,
            onBack = {
                if (currentStep > 0) {
                    currentStep--
                }
            })

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
            canAccept = steps[currentStep].isComplete,
            onAcceptClick = {
                when (isLastStep) {
                    true -> {
                        preferences.hasShownOnboarding().set(true)
                        router.popCurrentController()
                    }
                    false -> currentStep++
                }
            }) {
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
                        steps[step].Content()
                    }
                }
            }
    }
}
