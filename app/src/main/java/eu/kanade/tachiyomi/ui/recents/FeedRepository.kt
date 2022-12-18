package eu.kanade.tachiyomi.ui.recents

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.loggycat
import eu.kanade.tachiyomi.util.toDisplayManga
import logcat.LogPriority
import org.nekomanga.domain.chapter.FeedChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepository(
    private val db: DatabaseHelper = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
) {

    suspend fun getPage(offset: Int, type: FeedScreenType): Result<Pair<Boolean, List<FeedChapter>>, ResultError.Generic> {
        return com.github.michaelbull.result.runCatching {
            when (type) {
                FeedScreenType.Updates -> {
                    val chapters = db.getRecentChapters(offset = offset, isResuming = false).executeAsBlocking()
                        .mapNotNull {
                            FeedChapter(
                                mangaTitle = it.manga.title,
                                artwork = it.manga.toDisplayManga().currentArtwork,
                                simpleChapter = it.chapter.toSimpleChapter()!!,
                            )
                        }
                    Pair(chapters.isNotEmpty(), chapters)
                }
                else -> throw Exception("Not valid")
            }
        }.mapError { err ->
            this@FeedRepository.loggycat(LogPriority.ERROR, err)
            ResultError.Generic("Error : ${err.message}")
        }
    }
}
