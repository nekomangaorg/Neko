package eu.kanade.tachiyomi.source.online.merged.mangalife

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaLifeMangaDto(
    @SerialName("s") val name: String,
    @SerialName("al") val alternativeNames: List<String>,
    @SerialName("i") val id: String,
)

@Serializable
data class MangaLifeChapterDto(
    @SerialName("Chapter") val chapter: String,
    @SerialName("Type") val type: String,
    @SerialName("ChapterName") val chapterName: String?,
    @SerialName("Date") val date: String,
    @SerialName("Directory") val directory: String? = null,
    @SerialName("Page") val totalPages: Int? = null,
)
