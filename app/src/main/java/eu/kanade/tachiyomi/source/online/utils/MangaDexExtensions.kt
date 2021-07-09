package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.MangaDto

fun MangaDto.toBasicManga(): SManga {
    return SManga.create().apply {
        url = "/title/" + this@toBasicManga.data.id
        title = MdUtil.cleanString(this@toBasicManga.data.attributes.title["en"] ?: "")

        thumbnail_url = this@toBasicManga.relationships
            .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
            ?.attributes?.fileName
            ?.let { coverFileName ->
                MdUtil.cdnCoverUrl(this@toBasicManga.data.id, coverFileName)
            } ?: MdConstants.noCoverUrl
    }
}
