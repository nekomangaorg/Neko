package eu.kanade.tachiyomi.source.online.merged.suwayomi

import kotlinx.serialization.Serializable

@Serializable data class SuwayomiGraphQLResponseDto<T>(val data: T)

@Serializable data class SuwayomiSearchMangaDto(val mangas: SuwayomiNodesDto)

@Serializable data class SuwayomiNodesDto(val nodes: List<SuwayomiSeriesDto>)

@Serializable data class SuwayomiSeriesDto(val id: Long, val title: String)

@Serializable data class SuwayomiFetchChaptersDto(val fetchChapters: SuwayomiChaptersDto)

@Serializable data class SuwayomiChaptersDto(val chapters: List<SuwayomiChapterDto>)

@Serializable
data class SuwayomiChapterDto(
    val id: Long,
    val name: String,
    val chapterNumber: Float,
    val uploadDate: Long,
)

@Serializable data class SuwayomiFetchChapterPagesDto(val fetchChapterPages: SuwayomiPagesDto)

@Serializable data class SuwayomiPagesDto(val pages: List<String>)
