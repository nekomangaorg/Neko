package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {

    /**
     * Observes the user's library, applying user preferences for blocked scanlators and uploaders
     * on the fly.
     */
    fun observeLibrary(): Flow<List<LibraryManga>>

    /**
     * val libraryFlow = mangaRepository.getLibraryAsFlow() .stateIn( scope = viewModelScope,
     * started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList() )
     */
}
