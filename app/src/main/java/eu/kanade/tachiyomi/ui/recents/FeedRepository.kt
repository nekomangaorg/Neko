package eu.kanade.tachiyomi.ui.recents

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.loggycat
import eu.kanade.tachiyomi.util.toDisplayManga
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import logcat.LogPriority
import org.nekomanga.domain.chapter.FeedManga
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.network.ResultError
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepository(
    private val db: DatabaseHelper = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
) {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun getPage(offset: Int, type: FeedScreenType, group: FeedHistoryGroup): Result<Pair<Boolean, List<FeedManga>>, ResultError.Generic> {
        return com.github.michaelbull.result.runCatching {
            when (type) {
                FeedScreenType.Updates -> {
                    val chapters = db.getRecentChapters(offset = offset, isResuming = false).executeAsBlocking()
                        .mapNotNull {
                            it.manga.id ?: return@mapNotNull null
                            it.chapter.id ?: return@mapNotNull null
                            val simpleChapter = it.chapter.toSimpleChapter()!!
                            FeedManga(
                                mangaId = it.manga.id!!,
                                mangaTitle = it.manga.title,
                                date = simpleChapter.dateFetch,
                                artwork = it.manga.toDisplayManga().currentArtwork,
                                simpleChapter = persistentListOf(simpleChapter),
                            )
                        }
                    Pair(chapters.isNotEmpty(), chapters)
                }

                FeedScreenType.History -> {
                    val chapters = db.getHistoryUngrouped(offset = offset, isResuming = false).executeOnIO().mapNotNull {
                        it.manga.id ?: return@mapNotNull null
                        it.chapter.id ?: return@mapNotNull null
                        val simpleChapter = it.chapter.toSimpleChapter(it.history.last_read)!!
                        it.history.last_read
                        FeedManga(
                            mangaId = it.manga.id!!,
                            mangaTitle = it.manga.title,
                            date = it.history.last_read,
                            artwork = it.manga.toDisplayManga().currentArtwork,
                            simpleChapter = persistentListOf(simpleChapter),
                        )
                    }
                    /*val dbChapters = when (group) {
                        FeedHistoryGroup.Never -> db.getHistoryUngrouped(offset = offset, isResuming = false).executeOnIO()
                        FeedHistoryGroup.Series -> db.getRecentChapters(offset = offset, isResuming = false).executeOnIO()
                        else -> {
                            dateFormat.applyPattern(
                                when (group == FeedHistoryGroup.Week) {
                                    true -> "yyyy-w"
                                    false -> "yyyy-MM-dd"
                                },
                            )
                            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7 + 1
                            dateFormat.calendar.firstDayOfWeek = dayOfWeek
                            db.getHistoryUngrouped(offset = offset, isResuming = false).executeOnIO().groupBy {
                                val date = it.history.last_read
                                it.manga.id to if (date <= 0L) "-1" else dateFormat.format(Date(date))
                            }.mapNotNull { (key, mchs) ->
                                val manga = mchs.first().manga
                                val chapters = mchs.map { mch ->
                                    ChapterHistory(mch.chapter, mch.history)
                                }.filterChaptersByScanlators(manga)
                                extraCount += mchs.size - chapters.size
                                if (chapters.isEmpty()) return@mapNotNull null
                                val lastAmount = if (groupChaptersHistory == GroupType.ByDay) {
                                    ENDLESS_LIMIT
                                } else {
                                    recentItems.size
                                }
                                val existingItem = recentItems.takeLast(lastAmount).find {
                                    val date = Date(it.mch.history.last_read)
                                    key == it.manga_id to dateFormat.format(date)
                                }?.takeIf { updatePageCount }
                                val sort = Comparator<ChapterHistory> { c1, c2 ->
                                    c2.history!!.last_read.compareTo(c1.history!!.last_read)
                                }
                                val (sortedChapters, firstChapter, subCount) =
                                    setupExtraChapters(existingItem, chapters, sort)
                                extraCount += subCount
                                if (firstChapter == null) return@mapNotNull null
                                mchs.find { firstChapter.id == it.chapter.id }?.also {
                                    it.extraChapters = sortedChapters
                                }
                            }
                        }

                    }*/

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
