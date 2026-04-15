package org.nekomanga.usecases.manga

import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.repository.MergeRepositoryImpl

class UpdateMangaAggregate(
    private val mergeRepository: MergeRepositoryImpl,
    private val sourceManager: SourceManager,
) {
    suspend operator fun invoke(mangaId: Long, mangaUrl: String, isFavorite: Boolean) {
        if (isFavorite) {
            sourceManager.mangaDex.getAggregate(MdUtil.getMangaUUID(mangaUrl)).onSuccess {
                aggregateDto ->
                mergeRepository.insertMangaAggregate(
                        MangaAggregateEntity(mangaId = mangaId, volumes = aggregateDto.volumes.toString())
                    )
            }
        } else {
            mergeRepository.deleteMangaAggregate(mangaId)
        }
    }
}
