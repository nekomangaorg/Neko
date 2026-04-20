package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import kotlinx.coroutines.flow.Flow

interface UploaderRepository {
    suspend fun getUploaderByName(name: String): UploaderImpl?

    fun observeUploadersByNames(names: List<String>): Flow<List<UploaderImpl>>

    suspend fun getUploadersByNames(names: List<String>): List<UploaderImpl>

    suspend fun insertUploaders(uploaders: List<UploaderImpl>)

    suspend fun deleteUploader(name: String)
}
