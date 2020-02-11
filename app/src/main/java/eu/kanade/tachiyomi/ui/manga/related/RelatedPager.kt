package eu.kanade.tachiyomi.ui.manga.related

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.catalogue.browse.Pager
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class RelatedPager(val manga: Manga, val source: Source) : Pager() {

    override fun requestNext(): Observable<MangasPage> {
        return source.fetchMangaRelatedObservable(currentPage, manga)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { onPageReceived(it) }
    }

}
