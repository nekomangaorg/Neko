package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.nekomanga.data.database.entity.SearchMetadataEntity

@Dao
interface SearchMetadataDao {
    @Query("SELECT * FROM search_metadata WHERE manga_id = :mangaId")
    suspend fun getSearchMetadataForManga(mangaId: Long): SearchMetadataEntity?

    @Query("SELECT * FROM search_metadata WHERE indexed_extra = :extra")
    suspend fun getSearchMetadataByIndexedExtra(extra: String): List<SearchMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchMetadata(metadata: SearchMetadataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchMetadatas(metadatas: List<SearchMetadataEntity>)

    @Query("DELETE FROM search_metadata WHERE manga_id = :mangaId")
    suspend fun deleteSearchMetadata(mangaId: Long)
}
