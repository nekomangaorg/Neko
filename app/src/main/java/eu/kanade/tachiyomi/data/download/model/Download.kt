package eu.kanade.tachiyomi.data.download.model

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.getHttpSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.manga.toSimpleManga
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.SimpleManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val PROGRESS_DELAY = 50L

data class Download(
    val source: HttpSource,
    val mangaItem: SimpleManga,
    val chapterItem: SimpleChapter,
) {

    var pages: List<Page>? = null

    var errorMessage: String? = null

    val totalProgress: Int
        get() = pages?.sumOf(Page::progress) ?: 0

    val downloadedImages: Int
        get() = pages?.count { it.status == Page.State.READY } ?: 0

    @Transient private val _statusFlow = MutableStateFlow(State.NOT_DOWNLOADED)

    @Transient val statusFlow = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(status) {
            _statusFlow.value = status
        }

    @Transient
    val progressFlow =
        flow {
                if (pages == null) {
                    emit(0)
                    while (pages == null) {
                        delay(PROGRESS_DELAY)
                    }
                }

                val progressFlows = pages!!.map(Page::progressFlow)
                emitAll(combine(progressFlows) { it.average().toInt() })
            }
            .distinctUntilChanged()
            .debounce(PROGRESS_DELAY)

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }

    companion object {

        // max progress
        const val MaxProgress = 100

        suspend fun fromChapterId(
            chapterId: Long,
            db: DatabaseHelper = Injekt.get(),
            sourceManager: SourceManager = Injekt.get(),
        ): Download? {
            val chapter = db.getChapter(chapterId).executeOnIO()
            chapter?.manga_id ?: return null
            val manga = db.getManga(chapter.manga_id!!).executeOnIO() ?: return null
            val source = chapter.getHttpSource(sourceManager)

            return Download(source, manga.toSimpleManga(), chapter.toSimpleChapter()!!)
        }
    }
}
