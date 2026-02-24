package eu.kanade.tachiyomi.source.online.merged.suwayomi

import kotlinx.serialization.Serializable

@Serializable
data class SuwayomiGraphQLDto<T>(val data: T?, val errors: List<SuwayomiErrorDto>? = null) {
    fun hasErrors(): Boolean {
        return !errors.isNullOrEmpty()
    }
}

@Serializable
data class SuwayomiGraphQLErrorsDto(val errors: List<SuwayomiErrorDto>? = null) {
    fun isGraphQLUnauthorized(): Boolean {
        return !errors.isNullOrEmpty() &&
            errors.any {
                it.message.contains("suwayomi.tachidesk.server.user.UnauthorizedException") ||
                    it.message == "Unauthorized"
            }
    }
}

@Serializable data class SuwayomiErrorDto(val message: String)

@Serializable data class SuwayomiRefreshTokenDto(val refreshToken: SuwayomiTokensDto)

@Serializable data class SuwayomiLoginDto(val login: SuwayomiTokensDto)

@Serializable data class SuwayomiTokensDto(val accessToken: String, val refreshToken: String?)

@Serializable data class SuwayomiSearchMangaDto(val mangas: SuwayomiNodesDto)

@Serializable data class SuwayomiNodesDto(val nodes: List<SuwayomiSeriesDto>)

@Serializable
data class SuwayomiSeriesDto(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val source: SuwayomiSourceDto?,
)

@Serializable data class SuwayomiSourceDto(val name: String, val lang: String)

@Serializable data class SuwayomiFetchChaptersDto(val fetchChapters: SuwayomiChaptersDto?)

@Serializable data class SuwayomiGetChaptersDto(val chapters: SuwayomiChapterNodesDto)

@Serializable data class SuwayomiChapterNodesDto(val nodes: List<SuwayomiChapterDto>)

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
