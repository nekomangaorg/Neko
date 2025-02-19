package org.nekomanga.presentation.screens

import kotlinx.serialization.Serializable

object Screens {

    object Settings {
        @Serializable object Main

        @Serializable object General

        @Serializable object Appearance

        @Serializable object Library

        @Serializable object DataStorage

        @Serializable object MangaDex

        @Serializable object MergeSource

        @Serializable object Reader

        @Serializable object Downloads

        @Serializable object Search

        @Serializable object Tracking

        @Serializable object Security

        @Serializable object Advanced
    }
}
