package org.nekomanga.presentation.screens.deepLink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.util.manga.MangaMappings
import java.math.BigInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.constants.MdConstants

class DeepLinkViewModel(private val mangaMappings: MangaMappings) : ViewModel() {

    private val _deepLinkState = MutableStateFlow<DeepLinkState>(DeepLinkState.Loading)
    val deepLinkState = _deepLinkState.asStateFlow()

    fun handleDeepLink(host: String, path: String, id: String) {
        viewModelScope.launch {
            val query =
                when {
                    host.contains("anilist", true) -> {
                        val dexId = mangaMappings.getMangadexUUID(id, "al")
                        if (dexId == null) {
                            MdConstants.DeepLinkPrefix.error +
                                "Unable to map MangaDex manga, no mapping entry found for AniList ID"
                        } else {
                            MdConstants.DeepLinkPrefix.manga + dexId
                        }
                    }
                    host.contains("myanimelist", true) -> {
                        val dexId = mangaMappings.getMangadexUUID(id, "mal")
                        if (dexId == null) {
                            MdConstants.DeepLinkPrefix.error +
                                "Unable to map MangaDex manga, no mapping entry found for MyAnimeList ID"
                        } else {
                            MdConstants.DeepLinkPrefix.manga + dexId
                        }
                    }
                    host.contains("mangaupdates", true) -> {
                        val base = BigInteger(id, 36)
                        val muID = base.toString(10)
                        val dexId = mangaMappings.getMangadexUUID(muID, "mu_new")
                        if (dexId == null) {
                            MdConstants.DeepLinkPrefix.error +
                                "Unable to map MangaDex manga, no mapping entry found for MangaUpdates ID"
                        } else {
                            MdConstants.DeepLinkPrefix.manga + dexId
                        }
                    }
                    path.equals("GROUP", true) -> {
                        MdConstants.DeepLinkPrefix.group + id
                    }
                    path.equals("AUTHOR", true) -> {
                        MdConstants.DeepLinkPrefix.author + id
                    }
                    path.equals("LIST", true) -> {
                        MdConstants.DeepLinkPrefix.list + id
                    }
                    else -> {
                        MdConstants.DeepLinkPrefix.manga + id
                    }
                }
            _deepLinkState.update { DeepLinkState.Success(query) }
        }
    }
}

sealed class DeepLinkState {
    object Loading : DeepLinkState()
    data class Success(val query: String) : DeepLinkState()
}
