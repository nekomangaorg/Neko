package org.nekomanga.usecases.manga

import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.executeOnIO

class UpdateMangaAggregate(
    private val db: DatabaseHelper,
    private val sourceManager: SourceManager,
) {
    suspend operator fun invoke(mangaId: Long, mangaUrl: String, isFavorite: Boolean) {
        if (isFavorite) {
            sourceManager.mangaDex.getAggregate(MdUtil.getMangaUUID(mangaUrl)).onSuccess {
                aggregateDto ->
                db.insertMangaAggregate(
                        MangaAggregate(mangaId = mangaId, volumes = aggregateDto.volumes.toString())
                    )
                    .executeOnIO()
            }
        } else {
            db.deleteMangaAggregate(mangaId).executeOnIO()
        }
    }
}
