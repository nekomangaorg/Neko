package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.LibraryManga
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import org.nekomanga.data.database.dao.LibraryDao
import org.nekomanga.data.database.mapper.toLibraryManga
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.site.MangaDexPreferences

class MangaRepositoryImpl(
    private val libraryDao: LibraryDao,
    private val mangaDexPreferences: MangaDexPreferences,
    private val libraryPreferences: LibraryPreferences,
    // Injecting the dispatcher is best practice for testing, defaulting to IO
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MangaRepository {

    override fun observeLibrary(): Flow<List<LibraryManga>> {
        return combine(
                libraryDao.getLibrary(),
                mangaDexPreferences.blockedGroups().changes(),
                mangaDexPreferences.blockedUploaders().changes(),
                libraryPreferences.chapterScanlatorFilterOption().changes(),
            ) { rawLibraryList, blockedGroups, blockedUploaders, scanlatorPref ->
                rawLibraryList.map { rawManga ->
                    rawManga.toLibraryManga(
                        blockedGroups = blockedGroups,
                        blockedUploaders = blockedUploaders,
                        scanlatorFilterOption = scanlatorPref,
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }
}
