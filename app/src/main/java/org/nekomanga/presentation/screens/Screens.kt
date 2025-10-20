package org.nekomanga.presentation.screens

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

object Screens {

    @Serializable data object Library : NavKey
    @Serializable data object Feed : NavKey
    @Serializable data object Browse : NavKey
    @Serializable data object Manga : NavKey
    @Serializable data object Display : NavKey


    object Settings {
        @Serializable data object Main : NavKey

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

        @Serializable data object Search : NavKey

        @Serializable data object Tracking : NavKey

        @Serializable data object Security : NavKey

        @Serializable data object Advanced : NavKey
    }
}
