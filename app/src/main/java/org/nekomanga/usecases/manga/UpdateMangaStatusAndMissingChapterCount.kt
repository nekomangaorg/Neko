package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.models.Chapter as DbChapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.getMissingChapters
import eu.kanade.tachiyomi.util.chapter.getVolumeNum
import eu.kanade.tachiyomi.util.chapter.isAvailable
import org.nekomanga.data.database.model.toChapter
import org.nekomanga.data.database.model.toEntity
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.logging.TimberKt

class UpdateMangaStatusAndMissingChapterCount(
    private val mangaRepository: MangaRepositoryImpl,
    private val chapterRepository: ChapterRepositoryImpl,
    private val downloadManager: DownloadManager,
) {
    suspend operator fun invoke(manga: Manga) {
        // This can fail due to a race condition
        val allChaps =
            try {
                chapterRepository.getChaptersForMangaSync(manga.id!!).map { it.toChapter() }
            } catch (e: Exception) {
                TimberKt.d { "Failed to get chapters: $e" }
                listOf()
            }
        val missingChapters =
            allChaps.mapNotNull { it.toSimpleChapter()?.toChapterItem() }.getMissingChapters().count

        var updated = false

        // Only check/update status if no chapters are marked as missing (equivalent to
        // missingChapters == null in your context)
        if (missingChapters.isEmpty()) {
            val shouldBeCompleted = isMangaStatusCompleted(allChaps, manga)

            if (shouldBeCompleted && manga.status != SManga.COMPLETED) {
                manga.status = SManga.COMPLETED
                updated = true
            }
        }

        // Update missing chapter count if it has changed
        if (missingChapters != manga.missing_chapters) {
            manga.missing_chapters = missingChapters
            updated = true
        }

        // Persist changes
        if (updated) mangaRepository.insertManga(manga.toEntity())
    }

    private fun isMangaStatusCompleted(chapters: List<DbChapter>, manga: Manga): Boolean {
        val cancelledOrCompleted =
            manga.status == SManga.PUBLICATION_COMPLETE || manga.status == SManga.CANCELLED

        return cancelledOrCompleted &&
            manga.last_chapter_number != null &&
            chapters.any { chapter ->
                val volumeNum = getVolumeNum(chapter)
                chapter.isAvailable(downloadManager, manga) &&
                    getChapterNum(chapter)?.toInt() == manga.last_chapter_number &&
                    (volumeNum == manga.last_volume_number ||
                        volumeNum == null ||
                        manga.last_volume_number == null)
            }
    }
}
