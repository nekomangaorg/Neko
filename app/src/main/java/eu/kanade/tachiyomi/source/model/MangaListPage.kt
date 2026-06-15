package eu.kanade.tachiyomi.source.model

import org.nekomanga.domain.manga.SourceManga

data class MangaListPage(
    val manga: List<SManga> = emptyList(),
    val hasNextPage: Boolean,
    val sourceManga: List<SourceManga> = listOf(),
)
