package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import org.nekomanga.domain.manga.SourceManga

fun MangaDataDto.toBasicManga(coverQuality: Int = 0, useNoCoverUrl: Boolean = true): SManga {
    return SManga.create().apply {
        url = "/title/" + this@toBasicManga.id
        title = MdUtil.cleanString(
            MdUtil.getTitle(
                this@toBasicManga.attributes.title,
                this@toBasicManga.attributes.originalLanguage,
            ),
        )

        thumbnail_url = this@toBasicManga.relationships
            .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
            ?.attributes?.fileName
            ?.let { coverFileName ->
                MdUtil.cdnCoverUrl(this@toBasicManga.id, coverFileName, coverQuality)
            } ?: if (useNoCoverUrl) MdConstants.noCoverUrl else null
    }
}

fun MangaDataDto.toSourceManga(coverQuality: Int = 0, useNoCoverUrl: Boolean = true): SourceManga {
    val thumbnail = this@toSourceManga.relationships
        .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
        ?.attributes?.fileName
        ?.let { coverFileName ->
            MdUtil.cdnCoverUrl(this@toSourceManga.id, coverFileName, coverQuality)
        } ?: if (useNoCoverUrl) MdConstants.noCoverUrl else ""

    return SourceManga(
        url = "/title/" + this@toSourceManga.id,
        title = MdUtil.cleanString(
            MdUtil.getTitle(
                this@toSourceManga.attributes.title,
                this@toSourceManga.attributes.originalLanguage,
            ),
        ),
        currentThumbnail = thumbnail,
    )
}
