package org.nekomanga.data.repository

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import org.nekomanga.domain.browse.BrowseHomeSection
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError

interface BrowseRepository {
    fun isLoggedIn(): Boolean

    fun isLoggedInFlow(): Flow<Boolean>

    suspend fun getRandomManga(): Result<DisplayManga, ResultError>

    suspend fun getSearchPage(
        page: Int,
        filters: DexFilters,
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError>

    suspend fun getHomePage(): Result<List<BrowseHomeSection>, ResultError>

    suspend fun getFollows(): Result<List<DisplayManga>, ResultError>
}
