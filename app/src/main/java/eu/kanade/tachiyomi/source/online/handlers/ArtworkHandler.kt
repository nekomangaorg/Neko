package eu.kanade.tachiyomi.source.online.handlers

import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class ArtworkHandler {
    val preferences: PreferencesHelper by injectLazy()
    val statusHandler: StatusHandler by injectLazy()
    val service: MangaDexService by lazy { Injekt.get<NetworkHelper>().service }

    suspend fun getArtwork(mangaId: Long, mangaUUID: String): List<ArtworkImpl> {
        var offset = 0
        val result = service.viewArtwork(mangaUUID = mangaUUID, limit = MdUtil.artworkLimit, offset = offset).onFailure { }.getOrThrow()
        val artwork = result.data.toMutableList()
        while (result.total - offset > 0) {
            offset += MdUtil.artworkLimit
            val result2 = service.viewArtwork(mangaUUID = mangaUUID, limit = MdUtil.artworkLimit, offset = offset).onFailure { }.getOrThrow()
            artwork += result2.data
        }
        return artwork.map {
            ArtworkImpl(
                mangaId = mangaId,
                fileName = it.attributes!!.fileName!!,
                locale = it.attributes.locale ?: "",
                volume = if (it.attributes.volume != null) "Vol. ${it.attributes.volume}" else "",
                description = it.attributes.description ?: "",
            )
        }
    }
}
