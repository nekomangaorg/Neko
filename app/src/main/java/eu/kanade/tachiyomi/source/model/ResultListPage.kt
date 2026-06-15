package eu.kanade.tachiyomi.source.model

import org.nekomanga.domain.SourceResult

data class ResultListPage(
    val hasNextPage: Boolean,
    val results: List<SourceResult> = listOf(),
)
