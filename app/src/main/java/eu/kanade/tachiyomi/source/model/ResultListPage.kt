package eu.kanade.tachiyomi.source.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.SourceResult

data class ResultListPage(
    val hasNextPage: Boolean,
    val results: ImmutableList<SourceResult> = persistentListOf(),
)
