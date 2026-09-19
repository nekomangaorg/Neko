package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterDifference
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.toManga
import org.nekomanga.presentation.screens.reader.viewer.ChapterTransitionUiModel

/**
 * Domain interactor that prepares an immutable [ChapterTransitionUiModel] by evaluating chapter
 * difference / missing chapter count and download statuses via [DownloadManager].
 */
class ResolveChapterTransitionUiModelUseCase(private val downloadManager: DownloadManager) {

    operator fun invoke(
        transition: ChapterTransition,
        manga: MangaItem?,
    ): ChapterTransitionUiModel {
        val legacyManga = manga?.toManga()
        val isFromDownloaded =
            if (legacyManga != null) {
                downloadManager.isChapterDownloaded(transition.from.chapter, legacyManga)
            } else {
                false
            }

        return when (transition) {
            is ChapterTransition.Prev -> {
                val to = transition.to
                val isToDownloaded =
                    if (to != null && legacyManga != null) {
                        downloadManager.isChapterDownloaded(to.chapter, legacyManga)
                    } else {
                        false
                    }
                val diff =
                    if (to != null && hasMissingChapters(transition.from, to)) {
                        calculateChapterDifference(transition.from, to).toInt()
                    } else {
                        0
                    }
                ChapterTransitionUiModel.Prev(
                    fromChapterName = transition.from.chapter.name,
                    isFromDownloaded = isFromDownloaded,
                    toChapter =
                        to?.let {
                            ChapterTransitionUiModel.TargetChapterInfo(
                                chapterId = it.chapter.id ?: -1L,
                                name = it.chapter.name,
                                isDownloaded = isToDownloaded,
                                readerChapter = it,
                            )
                        },
                    missingChaptersCount = diff,
                )
            }
            is ChapterTransition.Next -> {
                val to = transition.to
                val isToDownloaded =
                    if (to != null && legacyManga != null) {
                        downloadManager.isChapterDownloaded(to.chapter, legacyManga)
                    } else {
                        false
                    }
                val diff =
                    if (to != null && hasMissingChapters(to, transition.from)) {
                        calculateChapterDifference(to, transition.from).toInt()
                    } else {
                        0
                    }
                ChapterTransitionUiModel.Next(
                    fromChapterName = transition.from.chapter.name,
                    isFromDownloaded = isFromDownloaded,
                    toChapter =
                        to?.let {
                            ChapterTransitionUiModel.TargetChapterInfo(
                                chapterId = it.chapter.id ?: -1L,
                                name = it.chapter.name,
                                isDownloaded = isToDownloaded,
                                readerChapter = it,
                            )
                        },
                    missingChaptersCount = diff,
                )
            }
        }
    }
}
