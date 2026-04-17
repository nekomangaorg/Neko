package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.BrowseFilterDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toImpl

class BrowseFilterRepositoryImpl(private val browseFilterDao: BrowseFilterDao) :
    BrowseFilterRepository {

    override fun observeBrowseFilters(): Flow<List<BrowseFilterImpl>> {
        return browseFilterDao.observeBrowseFilters().map { entities ->
            entities.map { it.toImpl() }
        }
    }

    override suspend fun getBrowseFilters(): List<BrowseFilterImpl> {
        return browseFilterDao.getBrowseFilters().map { it.toImpl() }
    }

    override suspend fun getDefaultFilter(): List<BrowseFilterImpl> {
        return browseFilterDao.getDefaultFilter().map { it.toImpl() }
    }

    override suspend fun insertBrowseFilter(filter: BrowseFilterImpl) {
        browseFilterDao.insertBrowseFilter(filter.toEntity())
    }

    override suspend fun insertBrowseFilters(filters: List<BrowseFilterImpl>) {
        browseFilterDao.insertBrowseFilters(filters.map { it.toEntity() })
    }

    override suspend fun deleteBrowseFilterByName(name: String) {
        browseFilterDao.deleteBrowseFilterByName(name)
    }

    override suspend fun deleteAllBrowseFilters() {
        browseFilterDao.deleteAllBrowseFilters()
    }
}
