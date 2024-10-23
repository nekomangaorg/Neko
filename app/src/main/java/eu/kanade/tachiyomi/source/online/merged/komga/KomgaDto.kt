package eu.kanade.tachiyomi.source.online.merged.komga

import kotlinx.serialization.Serializable

@Serializable data class KomgaPageDto(val number: Int, val fileName: String, val mediaType: String)

@Serializable
data class KomgaSeriesDto(
    val id: String,
    val libraryId: String,
    val name: String,
    val created: String?,
    val lastModified: String?,
    val fileLastModified: String,
    val booksCount: Int,
    val metadata: KomgaSeriesMetadataDto,
)

@Serializable data class KomgaSeriesMetadataDto(val title: String)

@Serializable
data class KomgaPaginatedResponseDto<T>(
    val content: List<T>,
    val empty: Boolean,
    val first: Boolean,
    val last: Boolean,
    val number: Long,
    val numberOfElements: Long,
    val size: Long,
    val totalElements: Long,
    val totalPages: Long,
)

@Serializable
data class KomgaBookDto(
    val id: String,
    val seriesId: String,
    val seriesTitle: String,
    val name: String,
    val number: Float,
    val created: String?,
    val lastModified: String?,
    val fileLastModified: String,
    val sizeBytes: Long,
    val size: String,
    val metadata: KomgaBookMetadataDto,
)

@Serializable
data class KomgaBookMetadataDto(
    val title: String,
    val titleLock: Boolean,
    val summary: String,
    val summaryLock: Boolean,
    val number: String,
    val numberLock: Boolean,
    val numberSort: Float,
    val numberSortLock: Boolean,
    val releaseDate: String?,
    val releaseDateLock: Boolean,
    val authors: List<KomgaAuthorDto>,
    val authorsLock: Boolean,
)

@Serializable data class KomgaAuthorDto(val name: String, val role: String)
