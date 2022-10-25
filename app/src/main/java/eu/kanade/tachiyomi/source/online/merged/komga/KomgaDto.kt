package eu.kanade.tachiyomi.source.online.merged.komga

import kotlinx.serialization.Serializable

@Serializable
data class KomgaPageDto(
    val number: Int,
    val fileName: String,
    val mediaType: String,
)

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

@Serializable
data class KomgaSeriesMetadataDto(
    val title: String,
)

@Serializable
data class KomgaPageWrapperDto<T>(
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

