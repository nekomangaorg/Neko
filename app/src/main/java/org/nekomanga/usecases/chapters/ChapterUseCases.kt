package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ChapterUseCases(private val db: DatabaseHelper = Injekt.get()) {
    val markChapters = MarkChapterUseCase(db)
}
