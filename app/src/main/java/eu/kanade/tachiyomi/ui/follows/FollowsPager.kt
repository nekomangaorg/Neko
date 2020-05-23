package eu.kanade.tachiyomi.ui.follows

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.ui.source.browse.Pager
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class FollowsPager(val source: Source) : Pager() {

    override fun requestNext(): Observable<MangasPage> {
        return source.fetchFollows(currentPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { onPageReceived(it) }
    }
}
