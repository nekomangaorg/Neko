package org.nekomanga.data.database.repository

import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.dao.ScanlatorGroupDao
import org.nekomanga.data.database.dao.UploaderDao
import org.nekomanga.data.database.entity.ScanlatorGroupEntity
import org.nekomanga.data.database.entity.UploaderEntity

class ScanlatorRepositoryImpl(
    private val scanlatorDao: ScanlatorGroupDao,
    private val uploaderDao: UploaderDao,
) {
    // Scanlator Group methods
    suspend fun getScanlatorGroupByName(name: String): ScanlatorGroupEntity? =
        scanlatorDao.getScanlatorGroupByName(name)

    suspend fun getScanlatorGroupsByNames(names: List<String>): List<ScanlatorGroupEntity> =
        scanlatorDao.getScanlatorGroupsByNames(names)

    suspend fun insertScanlatorGroup(group: ScanlatorGroupEntity): Long =
        scanlatorDao.insertScanlatorGroup(group)

    suspend fun insertScanlatorGroups(groups: List<ScanlatorGroupEntity>) =
        scanlatorDao.insertScanlatorGroups(groups)

    suspend fun deleteScanlatorGroup(name: String) = scanlatorDao.deleteScanlatorGroup(name)

    // Uploader methods
    suspend fun getUploaderByName(name: String): UploaderEntity? =
        uploaderDao.getUploaderByName(name)

    fun getUploadersByNames(names: List<String>): Flow<List<UploaderEntity>> =
        uploaderDao.getUploadersByNames(names)

    suspend fun insertUploaders(uploaders: List<UploaderEntity>) =
        uploaderDao.insertUploaders(uploaders)

    suspend fun deleteUploader(name: String) = uploaderDao.deleteUploader(name)
}
