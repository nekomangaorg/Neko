package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import org.nekomanga.data.database.dao.ScanlatorGroupDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toImpl

class ScanlatorGroupRepositoryImpl(private val scanlatorGroupDao: ScanlatorGroupDao) :
    ScanlatorGroupRepository {

    override suspend fun getScanlatorGroupByName(name: String): ScanlatorGroupImpl? {
        return scanlatorGroupDao.getScanlatorGroupByName(name)?.toImpl()
    }

    override suspend fun getScanlatorGroupsByNames(names: List<String>): List<ScanlatorGroupImpl> {
        return scanlatorGroupDao.getScanlatorGroupsByNames(names).map { it.toImpl() }
    }

    override suspend fun insertScanlatorGroup(group: ScanlatorGroupImpl): Long {
        return scanlatorGroupDao.insertScanlatorGroup(group.toEntity())
    }

    override suspend fun insertScanlatorGroups(groups: List<ScanlatorGroupImpl>) {
        scanlatorGroupDao.insertScanlatorGroups(groups.map { it.toEntity() })
    }

    override suspend fun deleteScanlatorGroup(name: String) {
        scanlatorGroupDao.deleteScanlatorGroup(name)
    }
}
