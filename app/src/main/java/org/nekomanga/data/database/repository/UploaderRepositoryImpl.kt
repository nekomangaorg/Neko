package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.UploaderDao
import org.nekomanga.data.database.mapper.toEntity
import org.nekomanga.data.database.mapper.toImpl

class UploaderRepositoryImpl(private val uploaderDao: UploaderDao) : UploaderRepository {

    override suspend fun getUploaderByName(name: String): UploaderImpl? {
        return uploaderDao.getUploaderByNameSync(name)?.toImpl()
    }

    override fun observeUploadersByNames(names: List<String>): Flow<List<UploaderImpl>> {
        return uploaderDao.getUploadersByNames(names).map { entities ->
            entities.map { it.toImpl() }
        }
    }

    override suspend fun getUploadersByNames(names: List<String>): List<UploaderImpl> {
        return uploaderDao.getUploadersByNamesSync(names).map { it.toImpl() }
    }

    override suspend fun insertUploaders(uploaders: List<UploaderImpl>) {
        uploaderDao.insertUploaders(uploaders.map { it.toEntity() })
    }

    override suspend fun deleteUploader(name: String) {
        uploaderDao.deleteUploader(name)
    }
}
