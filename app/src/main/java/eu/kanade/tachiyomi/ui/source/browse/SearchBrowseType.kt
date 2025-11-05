package eu.kanade.tachiyomi.ui.source.browse

import kotlinx.serialization.Serializable

@Serializable
sealed class SearchType {
    @Serializable object Title : SearchType()

    @Serializable object Creator : SearchType()

    @Serializable object Tag : SearchType()
}

@Serializable data class SearchBrowse(val type: SearchType, val query: String)
