package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter as DbChapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.getVolumeNum
import eu.kanade.tachiyomi.util.getMissingChapters
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.logging.TimberKt

class UpdateMangaStatusAndMissingChapterCount(
    private val db: DatabaseHelper,
    private val downloadManager: DownloadManager,
) {
    suspend operator fun invoke(manga: Manga) {
        // This can fail due to a race condition
        val allChaps = try {
            db.getChapters(manga).executeOnIO()
        } catch(e: Exception) {
            TimberKt.d { "Failed to get chapters: $e" }
            listOf()
        }
        val missingChapters =
            allChaps.map { it.toSimpleChapter()!!.toChapterItem() }.getMissingChapters().count

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
        if (updated) db.insertManga(manga).executeOnIO()
    }

    private fun isMangaStatusCompleted(chapters: List<DbChapter>, manga: Manga): Boolean {
        val cancelledOrCompleted =
            manga.status == SManga.PUBLICATION_COMPLETE || manga.status == SManga.CANCELLED

        if (cancelledOrCompleted && manga.last_chapter_number != null) {
            val final =
                chapters
                    .filter { it.isAvailable(downloadManager, manga) }
                    .filter { getChapterNum(it)?.toInt() == manga.last_chapter_number }
                    .filter {
                        getVolumeNum(it) == manga.last_volume_number ||
                            getVolumeNum(it) == null ||
                            manga.last_volume_number == null
                    }
            if (final.isNotEmpty()) {
                return true
            }
        }
        return false
    }
}
