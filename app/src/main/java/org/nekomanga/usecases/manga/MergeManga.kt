package org.nekomanga.usecases.manga

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapterOfType
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt

class SearchMergedManga(private val sourceManager: SourceManager) {
    suspend fun execute(
        query: String,
        mergeType: MergeType,
    ): Result<List<SourceMergeManga>, String> {
        return runCatching {
                val source = MergeType.getSource(mergeType, sourceManager)
                source.searchManga(query).map {
                    SourceMergeManga(
                        coverUrl = it.thumbnail_url ?: "",
                        title = it.title,
                        url = it.url,
                        mergeType = mergeType,
                    )
                }
            }
            .mapError { e ->
                TimberKt.e(e) { "Error searching merged manga" }
                e.message ?: "Error looking up information"
            }
    }
}

class RemoveMergedManga(
    private val db: DatabaseHelper,
    private val downloadManager: DownloadManager,
    private val libraryPreferences: LibraryPreferences,
) {
    suspend fun execute(mangaId: Long, mergeType: MergeType) {
        val dbManga = db.getManga(mangaId).executeOnIO() ?: return
        db.deleteMergeManga(mangaId).executeOnIO()
        val (mergedChapters, _) =
            db.getChapters(dbManga).executeOnIO().partition { it.isMergedChapterOfType(mergeType) }
        if (!libraryPreferences.enableLocalChapters().get()) {
            try {
                downloadManager.deleteChapters(dbManga, mergedChapters)
            } catch (e: Exception) {
                TimberKt.e(e) { "Failed to delete chapters for merged manga" }
            }
        }
        db.deleteChapters(mergedChapters).executeOnIO()
    }
}

class AddMergedManga(private val db: DatabaseHelper) {
    suspend fun execute(mangaId: Long, mergeManga: SourceMergeManga) {
        val newMergedManga = mergeManga.toMergeMangaImpl(mangaId)
        db.insertMergeManga(newMergedManga).executeOnIO()
    }
}
