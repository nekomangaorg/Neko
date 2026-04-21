package org.nekomanga.usecases.manga

import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.nekomanga.data.database.repository.MangaAggregateRepository

class UpdateMangaAggregate(
    private val mangaAggregateRepository: MangaAggregateRepository,
    private val sourceManager: SourceManager,
) {
    suspend operator fun invoke(mangaId: Long, mangaUrl: String, isFavorite: Boolean) {
        if (isFavorite) {
            sourceManager.mangaDex.getAggregate(MdUtil.getMangaUUID(mangaUrl)).onSuccess {
                aggregateDto ->
                mangaAggregateRepository.insertMangaAggregate(
                    MangaAggregate(mangaId = mangaId, volumes = aggregateDto.volumes.toString())
                )
            }
        } else {
            mangaAggregateRepository.deleteMangaAggregate(mangaId)
        }
    }
}
