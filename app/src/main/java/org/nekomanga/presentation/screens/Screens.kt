package org.nekomanga.presentation.screens

import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.source.browse.SearchBrowse
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import kotlinx.serialization.Serializable

object Screens {

    @Serializable data object Onboarding : NavKey

    @Serializable data class Library(val initialSearch: String = "") : NavKey

    @Serializable data object Feed : NavKey

    @Serializable data object Splash : NavKey

    @Serializable data object Stats : NavKey

    @Serializable data object About : NavKey

    @Serializable data object License : NavKey

    @Serializable data class WebView(val title: String, val url: String) : NavKey

    @Serializable data class Browse(val searchBrowse: SearchBrowse? = null) : NavKey

    @Serializable data class Manga(val mangaId: Long) : NavKey

    @Serializable data class Display(val displayScreenType: DisplayScreenType) : NavKey

    @Serializable data class Similar(val mangaUUID: String) : NavKey

    object Settings {
        @Serializable data class Main(val deepLink: NavKey? = null) : NavKey

        @Serializable data object General : NavKey

        @Serializable data object Appearance : NavKey

        @Serializable data object Categories : NavKey

        @Serializable data object Debug : NavKey

        @Serializable data object Library : NavKey

        @Serializable data object DataStorage : NavKey

        @Serializable data object MangaDex : NavKey

        @Serializable data object MergeSource : NavKey

        @Serializable data object Reader : NavKey

        @Serializable data object Downloads : NavKey

        @Serializable data object Tracking : NavKey

        @Serializable data object Security : NavKey

        @Serializable data object Advanced : NavKey
    }
}
