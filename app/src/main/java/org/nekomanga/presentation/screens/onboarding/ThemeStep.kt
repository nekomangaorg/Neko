package org.nekomanga.presentation.screens.onboarding

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.Themes
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import eu.kanade.tachiyomi.util.system.isInNightMode
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.theme.ThemeItem
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ThemeStep : OnboardingStep {
    override val isComplete: Boolean = true

    private val preferences: PreferencesHelper = Injekt.get()

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val nightModePreference = preferences.nightMode()

        val nightMode by nightModePreference.collectAsState()

        val followingSystemTheme by
            remember(nightMode) {
                derivedStateOf { nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
            }

        val darkAppTheme by preferences.darkTheme().collectAsState()
        val lightAppTheme by preferences.lightTheme().collectAsState()
        val supportsDynamic = DynamicColors.isDynamicColorAvailable()

        Themes.entries
            .filter {
                (!it.isDarkTheme || it.followsSystem) &&
                    (it.styleRes != R.style.Theme_Tachiyomi_Monet || supportsDynamic)
            }
            .toSet()

        val lightThemes by remember {
            derivedStateOf {
                Themes.entries
                    .filter {
                        (!it.isDarkTheme || it.followsSystem) &&
                            (it.styleRes != R.style.Theme_Tachiyomi_Monet || supportsDynamic)
                    }
                    .toSet()
            }
        }

        val darkThemes by remember {
            derivedStateOf {
                Themes.entries
                    .filter {
                        (it.isDarkTheme || it.followsSystem) &&
                            (it.styleRes != R.style.Theme_Tachiyomi_Monet || supportsDynamic)
                    }
                    .toSet()
            }
        }

        Column(modifier = Modifier.padding(Size.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.follow_system_theme))
                Switch(
                    checked = nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                    colors =
                        SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                    onCheckedChange = {
                        when (it) {
                            true -> {
                                preferences
                                    .nightMode()
                                    .set(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                (context as? Activity)?.let { activity ->
                                    ActivityCompat.recreate(activity)
                                }
                            }
                            false -> preferences.nightMode().set(context.appDelegateNightMode())
                        }
                    },
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Size.medium),
            ) {
                lightThemes.forEach { theme ->
                    val isSelected =
                        remember(darkAppTheme, lightAppTheme, nightMode) {
                            isSelected(theme, false, darkAppTheme, lightAppTheme, nightMode)
                        }
                    ThemeItem(
                        theme = theme,
                        isDarkTheme = false,
                        selected = isSelected,
                        onClick = {
                            themeClicked(
                                theme,
                                context,
                                isSelected = isSelected,
                                followingSystemTheme = followingSystemTheme,
                                isDarkTheme = false,
                            )
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Size.medium),
            ) {
                darkThemes.forEach { theme ->
                    val isSelected =
                        remember(darkAppTheme, lightAppTheme, nightMode) {
                            isSelected(theme, true, darkAppTheme, lightAppTheme, nightMode)
                        }
                    ThemeItem(
                        theme = theme,
                        isDarkTheme = true,
                        selected = isSelected,
                        onClick = {
                            themeClicked(
                                theme,
                                context,
                                isSelected = isSelected,
                                followingSystemTheme = followingSystemTheme,
                                isDarkTheme = true,
                            )
                        },
                    )
                }
            }
        }
    }

    fun isSelected(
        theme: Themes,
        isDarkTheme: Boolean,
        darkAppTheme: Themes,
        lightAppTheme: Themes,
        nightMode: Int,
    ): Boolean {
        return when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> darkAppTheme == theme && isDarkTheme
            AppCompatDelegate.MODE_NIGHT_NO -> lightAppTheme == theme && !isDarkTheme
            else ->
                (darkAppTheme == theme && isDarkTheme) || (lightAppTheme == theme && !isDarkTheme)
        }
    }

    private fun themeClicked(
        theme: Themes,
        context: Context,
        isSelected: Boolean,
        followingSystemTheme: Boolean,
        isDarkTheme: Boolean,
    ) {
        TimberKt.d {
            """
                isSelected: $isSelected
                isDarkTheme : $isDarkTheme
                followingSystemTheme: $followingSystemTheme
                isInNightMode: ${context.isInNightMode()}
            """
                .trimIndent()
        }

        val nightMode =
            when (isDarkTheme) {
                true -> {
                    preferences.darkTheme().set(theme)
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                false -> {
                    preferences.lightTheme().set(theme)
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            }

        if (followingSystemTheme && isSelected) {
            preferences.nightMode().set(nightMode)
        } else if (!followingSystemTheme) {
            preferences.nightMode().set(nightMode)
        }

        (context as? Activity)?.let { activity -> ActivityCompat.recreate(activity) }
    }
}
