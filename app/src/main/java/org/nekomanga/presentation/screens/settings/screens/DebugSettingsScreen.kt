package org.nekomanga.presentation.screens.settings.screens

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.analytics.FirebaseAnalytics
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.settings.Preference

internal class DebugSettingsScreen(
    val toastEvent: SharedFlow<UiText>,
    val unfollowAllLibraryManga: () -> Unit,
    val removeAllMangaWithStatusOnMangaDex: () -> Unit,
    val clearAllManga: () -> Unit,
    val clearAllCategories: () -> Unit,
    val clearAllTrackers: () -> Unit,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.advanced

    @SuppressLint("BatteryLife")
    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        val context = LocalContext.current

        LaunchedEffect(Unit) { toastEvent.collect { event -> context.toast(event) } }

        return persistentListOf(
            Preference.PreferenceItem.TextPreference(
                title = "Send a test firebase event",
                onClick = {
                    FirebaseAnalytics.getInstance(context)
                        .logEvent("test_event", Bundle().apply { this.putString("test", "test") })
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Unfollow all library manga",
                onClick = unfollowAllLibraryManga,
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Remove all manga with status on MangaDex",
                onClick = removeAllMangaWithStatusOnMangaDex,
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Clear all Manga",
                onClick = clearAllManga,
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Clear all categories",
                onClick = clearAllCategories,
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Clear all trackers",
                onClick = clearAllTrackers,
            ),
        )
    }
}
