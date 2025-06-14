package eu.kanade.tachiyomi.source.online.merged.suwayomi

import kotlinx.serialization.Serializable

@Serializable data class SuwayomiGraphQLDto<T>(val data: T)

@Serializable data class SuwayomiSearchMangaDto(val mangas: SuwayomiNodesDto)

@Serializable data class SuwayomiNodesDto(val nodes: List<SuwayomiSeriesDto>)

@Serializable
data class SuwayomiSeriesDto(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
    val source: SuwayomiSourceDto,
)

@Serializable data class SuwayomiSourceDto(val name: String, val lang: String)

@Serializable data class SuwayomiFetchChaptersDto(val fetchChapters: SuwayomiChaptersDto)

@Serializable data class SuwayomiChaptersDto(val chapters: List<SuwayomiChapterDto>)

@Serializable
data class SuwayomiChapterDto(
    val id: Long,
    val name: String,
    val chapterNumber: Float,
    val uploadDate: Long,
    val sourceOrder: Long,
    val isRead: Boolean,
    val scanlator: String?,
)

@Serializable data class SuwayomiFetchChapterPagesDto(val fetchChapterPages: SuwayomiPagesDto)

@Serializable data class SuwayomiPagesDto(val pages: List<String>)
