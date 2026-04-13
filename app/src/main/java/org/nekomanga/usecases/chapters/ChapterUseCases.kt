package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Chapter handling */
class ChapterUseCases(
    db: DatabaseHelper = Injekt.get(),
    statusHandler: StatusHandler = Injekt.get(),
    mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    calculateChapterFilter: CalculateChapterFilterUseCase = Injekt.get(),
) {
    val markChapters = MarkChapterUseCase(db)
    val markChaptersRemote = MarkChaptersRemote(statusHandler, mangaDexPreferences)
    val calculateChapterFilter = calculateChapterFilter
    val markPreviousChapters = MarkPreviousChaptersUseCase()
}
