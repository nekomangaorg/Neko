package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterDifference
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters

/**
 * Pure immutable presentation model for chapter transition pages in Compose readers. Decoupled from
 * DownloadManager, legacy Manga entity conversions, and in-UI chapter gap math.
 */
@Immutable
sealed interface ChapterTransitionUiModel {
    val fromChapterName: String
    val isFromDownloaded: Boolean

    @Immutable
    data class Prev(
        override val fromChapterName: String,
        override val isFromDownloaded: Boolean = false,
        val toChapter: TargetChapterInfo? = null,
        val missingChaptersCount: Int = 0,
    ) : ChapterTransitionUiModel

    @Immutable
    data class Next(
        override val fromChapterName: String,
        override val isFromDownloaded: Boolean = false,
        val toChapter: TargetChapterInfo? = null,
        val missingChaptersCount: Int = 0,
    ) : ChapterTransitionUiModel

    @Immutable
    data class TargetChapterInfo(
        val chapterId: Long,
        val name: String,
        val isDownloaded: Boolean = false,
        val preloadState: PreloadState = PreloadState.Ready,
    )

    @Immutable
    sealed interface PreloadState {
        @Immutable data object Ready : PreloadState

        @Immutable data object Loading : PreloadState

        @Immutable data class Error(val message: String = "") : PreloadState
    }

    companion object {
        fun from(transition: ChapterTransition): ChapterTransitionUiModel {
            return when (transition) {
                is ChapterTransition.Prev -> {
                    val to = transition.to
                    val diff =
                        if (to != null && hasMissingChapters(transition.from, to)) {
                            calculateChapterDifference(transition.from, to).toInt()
                        } else {
                            0
                        }
                    val preloadState =
                        when (val state = to?.state) {
                            is ReaderChapter.State.Loading -> PreloadState.Loading
                            is ReaderChapter.State.Error ->
                                PreloadState.Error(state.error.message ?: "")
                            else -> PreloadState.Ready
                        }
                    Prev(
                        fromChapterName = transition.from.chapter.name,
                        toChapter =
                            to?.let {
                                TargetChapterInfo(
                                    chapterId = it.chapter.id ?: -1L,
                                    name = it.chapter.name,
                                    preloadState = preloadState,
                                )
                            },
                        missingChaptersCount = diff,
                    )
                }
                is ChapterTransition.Next -> {
                    val to = transition.to
                    val diff =
                        if (to != null && hasMissingChapters(to, transition.from)) {
                            calculateChapterDifference(to, transition.from).toInt()
                        } else {
                            0
                        }
                    val preloadState =
                        when (val state = to?.state) {
                            is ReaderChapter.State.Loading -> PreloadState.Loading
                            is ReaderChapter.State.Error ->
                                PreloadState.Error(state.error.message ?: "")
                            else -> PreloadState.Ready
                        }
                    Next(
                        fromChapterName = transition.from.chapter.name,
                        toChapter =
                            to?.let {
                                TargetChapterInfo(
                                    chapterId = it.chapter.id ?: -1L,
                                    name = it.chapter.name,
                                    preloadState = preloadState,
                                )
                            },
                        missingChaptersCount = diff,
                    )
                }
            }
        }
    }
}
