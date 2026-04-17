package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import kotlinx.coroutines.flow.Flow

interface BrowseFilterRepository {

    /** Continuously observes all browse filters in the database. */
    fun observeBrowseFilters(): Flow<List<BrowseFilterImpl>>

    /** One-shot fetch of all browse filters. */
    suspend fun getBrowseFilters(): List<BrowseFilterImpl>

    /** One-shot fetch of the default browse filter(s). */
    suspend fun getDefaultFilter(): List<BrowseFilterImpl>

    /** Inserts a single browse filter. Replaces on conflict. */
    suspend fun insertBrowseFilter(filter: BrowseFilterImpl)

    /** Inserts a list of browse filters. Replaces on conflict. */
    suspend fun insertBrowseFilters(filters: List<BrowseFilterImpl>)

    /** Deletes a browse filter by its specific name. */
    suspend fun deleteBrowseFilterByName(name: String)

    /** Clears all browse filters from the database. */
    suspend fun deleteAllBrowseFilters()
}
