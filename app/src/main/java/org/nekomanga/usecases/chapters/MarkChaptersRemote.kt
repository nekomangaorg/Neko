package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions

/** Given a list of chapters this attempts to mark them read/unread on the MangaDex website */
class MarkChaptersRemote(
    private val statusHandler: StatusHandler,
    private val preferences: PreferencesHelper,
) {
    suspend operator fun invoke(
        markAction: ChapterMarkActions,
        mangaUuid: String,
        chapterItems: List<ChapterItem>,
        skipSync: Boolean,
    ) {
        val syncRead =
            when (markAction) {
                is ChapterMarkActions.Read -> true
                is ChapterMarkActions.Unread -> false
                else -> null
            }
        if (syncRead != null && !skipSync && preferences.readingSync().get()) {

            val chapterUuidList =
                chapterItems
                    .filter { !it.chapter.isMergedChapter() }
                    .map { it.chapter.mangaDexChapterId }
            withNonCancellableContext {
                if (chapterUuidList.isNotEmpty()) {
                    statusHandler.marksChaptersStatus(mangaUuid, chapterUuidList, syncRead)
                }
            }
        }
    }
}
