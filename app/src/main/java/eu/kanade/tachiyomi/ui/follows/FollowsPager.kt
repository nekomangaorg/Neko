package eu.kanade.tachiyomi.ui.follows

import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.ui.source.browse.Pager
import eu.kanade.tachiyomi.util.system.runAsObservable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class FollowsPager(val source: MangaDex) : Pager() {

    override fun requestNext(): Observable<MangaListPage> {
        return runAsObservable {
            source.fetchFollowList()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { onPageReceived(it) }
    }
}
