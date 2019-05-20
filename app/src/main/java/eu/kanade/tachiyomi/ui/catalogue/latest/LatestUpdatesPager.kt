package eu.kanade.tachiyomi.ui.catalogue.latest

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.ui.catalogue.browse.Pager
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class LatestUpdatesPager(val source: Source) : Pager() {

    override fun requestNext(): Observable<MangasPage> {
        return source.fetchLatestUpdates(currentPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { onPageReceived(it) }
    }

}
