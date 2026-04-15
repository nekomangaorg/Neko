package org.nekomanga.usecases.manga

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapterOfType
import org.nekomanga.data.database.model.toChapter
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.MergeRepositoryImpl
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt

class SearchMergedManga(private val sourceManager: SourceManager) {
    suspend fun execute(
        query: String,
        mergeType: MergeType,
    ): Result<List<SourceMergeManga>, String> {
        return com.github.michaelbull.result
            .runCatching {
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
    private val mangaRepository: MangaRepositoryImpl,
    private val chapterRepository: ChapterRepositoryImpl,
    private val mergeRepository: MergeRepositoryImpl,
    private val downloadManager: DownloadManager,
    private val libraryPreferences: LibraryPreferences,
) {
    suspend fun execute(mangaId: Long, mergeType: MergeType) {
        val dbManga = mangaRepository.getMangaByIdSync(mangaId)?.toManga() ?: return
        mergeRepository.deleteMergeMangaByType(mangaId, mergeType.id)
        val (mergedChapters, _) =
            chapterRepository
                .getChaptersForMangaSync(mangaId)
                .map { it.toChapter() }
                .partition { it.isMergedChapterOfType(mergeType) }
        if (!libraryPreferences.enableLocalChapters().get()) {
            try {
                downloadManager.deleteChapters(dbManga, mergedChapters)
            } catch (e: Exception) {
                TimberKt.e(e) { "Failed to delete chapters for merged manga" }
            }
        }
        chapterRepository.deleteChapters(mergedChapters.map { it.toEntity() })
    }
}

class AddMergedManga(private val mergeRepository: MergeRepositoryImpl) {
    suspend fun execute(mangaId: Long, mergeManga: SourceMergeManga) {
        val newMergedManga = mergeManga.toMergeMangaImpl(mangaId)
        mergeRepository.insertMergeManga(newMergedManga.toEntity())
    }
}
