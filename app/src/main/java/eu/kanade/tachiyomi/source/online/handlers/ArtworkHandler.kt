package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.online.models.dto.RelationshipDto
import eu.kanade.tachiyomi.source.online.models.dto.RelationshipDtoList
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.getOrResultError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class ArtworkHandler {
    val preferences: PreferencesHelper by injectLazy()
    val statusHandler: StatusHandler by injectLazy()
    val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }

    suspend fun getArtwork(mangaUUID: String): Result<List<SourceArtwork>, ResultError> {
        return withContext(Dispatchers.IO) {
            fetchArtwork(mangaUUID, 0)
                .andThen { relationshipDtoList ->
                    val artwork = relationshipDtoList.data.toMutableList()

                    Ok(
                        when (relationshipDtoList.total > relationshipDtoList.limit) {
                            true -> artwork + fetchRestOfArtwork(mangaUUID, relationshipDtoList.limit, relationshipDtoList.total)
                            false -> artwork
                        }
                            .map {
                                SourceArtwork(
                                    fileName = it.attributes!!.fileName!!,
                                    locale = it.attributes.locale ?: "",
                                    volume = if (it.attributes.volume != null) "Vol. ${it.attributes.volume}" else "",
                                    description = it.attributes.description ?: "",
                                )
                            },
                    )
                }
        }
    }

    private suspend fun fetchRestOfArtwork(mangaUUID: String, limit: Int, total: Int): List<RelationshipDto> {
        return withContext(Dispatchers.IO) {
            val totalRequestNo = (total / limit)
            (1..totalRequestNo).map { pos ->
                async {
                    fetchArtwork(mangaUUID, pos * limit)
                }
            }.awaitAll().mapNotNull { it.getOrElse { null } }.map { it.data }.flatten()
        }
    }

    private suspend fun fetchArtwork(mangaUUID: String, offset: Int): Result<RelationshipDtoList, ResultError> {
        return service.viewArtwork(mangaUUID = mangaUUID, limit = MdUtil.artworkLimit, offset = offset)
            .getOrResultError("Failed to get artwork")
    }
}
