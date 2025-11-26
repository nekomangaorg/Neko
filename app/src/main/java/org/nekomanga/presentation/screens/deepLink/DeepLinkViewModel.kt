package org.nekomanga.presentation.screens.deepLink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.toDisplayManga
import java.math.BigInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.network.message
import org.nekomanga.presentation.screens.Screens
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkViewModel() : ViewModel() {

    private val mangaMappings: MangaMappings = Injekt.get()
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex
    private val db: DatabaseHelper = Injekt.get()

    private val _deepLinkState = MutableStateFlow<DeepLinkState>(DeepLinkState.Loading)
    val deepLinkState = _deepLinkState.asStateFlow()

    fun handleDeepLink(host: String, path: String, id: String) {
        viewModelScope.launch {
            parseUri(host, path, id)
                .andThen { deepLinkType ->
                    when (deepLinkType) {
                        else -> {
                            getDeepLinkManga(deepLinkType.uuid).map { displayManga ->
                                DeepLinkState.Success(
                                    listOf(Screens.Browse(), Screens.Manga(displayManga.mangaId))
                                )
                            }
                        }
                    }
                }
                .onFailure { _deepLinkState.value = DeepLinkState.Error(it.message()) }
                .onSuccess { result -> _deepLinkState.value = result }
        }
    }

    suspend fun getDeepLinkManga(uuid: String): Result<DisplayManga, ResultError> {
        return mangaDex.searchForManga(uuid).andThen { mangaListPage ->
            val displayManga = mangaListPage.sourceManga.first().toDisplayManga(db, mangaDex.id)
            Ok(displayManga)
        }
    }

    private fun parseUri(
        host: String,
        path: String,
        id: String,
    ): Result<DeepLinkType, ResultError> {
        return when {
            host.contains("anilist", true) -> resolveMapping(id, "al", "AniList")
            host.contains("myanimelist", true) -> resolveMapping(id, "mal", "MyAnimeList")

            // Combine the Base36 logic
            host.contains("mangaupdates", true) -> {
                val convertedId = BigInteger(id, 36).toString()
                resolveMapping(convertedId, "mu_new", "MangaUpdates")
            }

            host.contains("kitsu", true) -> {
                val convertedId = BigInteger(id, 36).toString()
                resolveMapping(convertedId, "kitsu", "Kitsu")
            }

            // Direct matches
            path.equals("GROUP", true) -> Ok(DeepLinkType.Group(id))
            path.equals("AUTHOR", true) -> Ok(DeepLinkType.Author(id))
            path.equals("LIST", true) -> Ok(DeepLinkType.List(id))
            else -> Ok(DeepLinkType.Manga(id))
        }
    }

    private fun resolveMapping(
        lookupId: String,
        key: String,
        name: String,
    ): Result<DeepLinkType, ResultError> {
        val dexId = mangaMappings.getMangadexUUID(lookupId, key)
        return if (dexId != null) {
            Ok(DeepLinkType.Manga(dexId))
        } else {
            Err(
                ResultError.Generic(
                    "Unable to map MangaDex manga, no mapping entry found for $name ID"
                )
            )
        }
    }
}

sealed interface DeepLinkType {
    val uuid: String

    data class Author(override val uuid: String) : DeepLinkType

    data class Group(override val uuid: String) : DeepLinkType

    data class Manga(override val uuid: String) : DeepLinkType

    data class List(override val uuid: String) : DeepLinkType
}

sealed class DeepLinkState {
    object Loading : DeepLinkState()

    data class Error(val errorMessage: String) : DeepLinkState()

    data class Success(val screens: List<NavKey>) : DeepLinkState()
}
