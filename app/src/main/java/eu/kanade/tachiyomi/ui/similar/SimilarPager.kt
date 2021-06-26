package eu.kanade.tachiyomi.ui.manga.similar

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.ui.source.browse.NoResultsException
import eu.kanade.tachiyomi.ui.source.browse.Pager
import eu.kanade.tachiyomi.util.system.runAsObservable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class SimilarPager(val manga: Manga, val source: MangaDex) : Pager() {

    override fun requestNext(): Observable<MangaListPage> {
        return runAsObservable {
            source.fetchSimilarManga(manga, false)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                if (it.manga.isNotEmpty()) {
                    onPageReceived(it)
                } else {
                    throw NoResultsException()
                }
            }
    }
}
