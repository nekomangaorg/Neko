package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.BrowseFilterDao
import org.nekomanga.data.database.entity.BrowseFilterEntity

class BrowseFilterRepositoryImpl(
    private val browseFilterDao: BrowseFilterDao
) {
    fun getBrowseFilters(): Flow<List<BrowseFilterEntity>> = browseFilterDao.getBrowseFilters()

    suspend fun getDefaultFilter(): List<BrowseFilterEntity> = browseFilterDao.getDefaultFilter()

    suspend fun insertBrowseFilter(filter: BrowseFilterEntity): Long = browseFilterDao.insertBrowseFilter(filter)

    suspend fun insertBrowseFilters(filters: List<BrowseFilterEntity>) = browseFilterDao.insertBrowseFilters(filters)

    suspend fun deleteBrowseFilterByName(name: String) = browseFilterDao.deleteBrowseFilterByName(name)

    suspend fun deleteAllBrowseFilters() = browseFilterDao.deleteAllBrowseFilters()
}
