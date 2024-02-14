package org.nekomanga.presentation.screens.onboarding

import androidx.compose.runtime.Composable

internal interface OnboardingStep {

    val isComplete: Boolean

    @Composable fun Content()
}
