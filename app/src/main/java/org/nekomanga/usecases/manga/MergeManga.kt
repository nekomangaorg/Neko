package org.nekomanga.usecases.manga

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapterOfType
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MergeMangaRepository
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.logging.TimberKt

class SearchMergedManga(private val sourceManager: SourceManager) {
    suspend fun execute(
        query: String,
        mergeType: MergeType,
        mergedUrls: List<String> = emptyList(),
    ): Result<List<SourceMergeManga>, String> {
        return com.github.michaelbull.result
            .runCatching {
                val source = MergeType.getSource(mergeType, sourceManager)
                source.searchManga(query).mapNotNull {
                    val url = it.url
                    if (mergedUrls.contains(url)) return@mapNotNull null
                    SourceMergeManga(
                        coverUrl = it.thumbnail_url ?: "",
                        title = it.title,
                        url = url,
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
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
    private val mergeMangaRepository: MergeMangaRepository,
    private val downloadManager: DownloadManager,
    private val libraryPreferences: LibraryPreferences,
) {
    suspend fun execute(mangaId: Long, mergeManga: MergeMangaImpl): Boolean {
        val dbManga = mangaRepository.getMangaById(mangaId) ?: return true
        mergeMangaRepository.deleteMergeManga(mergeManga)
        val multiMerged =
            mergeMangaRepository.getMergeMangaList(mangaId).any {
                it.mergeType == mergeManga.mergeType && it.url != mergeManga.url
            }

        if (!multiMerged) {
            val (mergedChapters, _) =
                chapterRepository.getChaptersForManga(mangaId).partition {
                    it.isMergedChapterOfType(mergeManga.mergeType)
                }
            if (!libraryPreferences.enableLocalChapters().get()) {
                try {
                    downloadManager.deleteChapters(dbManga, mergedChapters)
                } catch (e: Exception) {
                    TimberKt.e(e) { "Failed to delete chapters for merged manga" }
                }
            }
            chapterRepository.deleteChapters(mergedChapters)
        }
        return !multiMerged
    }
}

class AddMergedManga(private val mergeMangaRepository: MergeMangaRepository) {
    suspend fun execute(mangaId: Long, mergeManga: SourceMergeManga) {
        val newMergedManga = mergeManga.toMergeMangaImpl(mangaId)
        mergeMangaRepository.insertMergeManga(newMergedManga)
    }
}
