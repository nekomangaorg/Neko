package eu.kanade.tachiyomi.source.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.domain.manga.SourceManga

data class MangaListPage(
    val manga: List<SManga> = emptyList(),
    val hasNextPage: Boolean,
    val sourceManga: ImmutableList<SourceManga> = persistentListOf(),
)
