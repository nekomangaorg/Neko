package eu.kanade.tachiyomi.ui.source.browse

import kotlinx.serialization.Serializable

@Serializable
sealed class SearchType {
    object Title : SearchType()

    object Creator : SearchType()

    object Tag : SearchType()
}

@Serializable data class SearchBrowse(val type: SearchType, val query: String)
