package org.nekomanga.presentation.screens

import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.source.latest.SerializableDisplayScreenType
import kotlinx.serialization.Serializable

@Serializable sealed interface Screen : NavKey

object Screens {

    @Serializable data object Onboarding : Screen

    @Serializable data class Library(val initialSearch: String = "") : Screen

    @Serializable data object Feed : Screen

    @Serializable data class Loading(val showLoadingIndicator: Boolean) : Screen

    @Serializable data object Stats : Screen

    @Serializable data object About : Screen

    @Serializable data object License : Screen

    @Serializable data class WebView(val title: String, val url: String) : Screen

    @Serializable data class Browse(val title: String? = null) : Screen

    @Serializable data class Manga(val mangaId: Long) : Screen

    @Serializable data class Display(val displayScreenType: SerializableDisplayScreenType) : Screen

    @Serializable data class Similar(val mangaUUID: String) : Screen

    @Serializable data class DeepLink(val host: String, val path: String, val id: String) : Screen

    object Settings {
        @Serializable data class Main(val deepLink: Screen? = null) : Screen

        @Serializable data object General : Screen

        @Serializable data object Appearance : Screen

        @Serializable data object Categories : Screen

        @Serializable data object Debug : Screen

        @Serializable data object Library : Screen

        @Serializable data object DataStorage : Screen

        @Serializable data object MangaDex : Screen

        @Serializable data object MergeSource : Screen

        @Serializable data object Reader : Screen

        @Serializable data object Downloads : Screen

        @Serializable data object Tracking : Screen

        @Serializable data object Security : Screen

        @Serializable data object Advanced : Screen
    }
}
