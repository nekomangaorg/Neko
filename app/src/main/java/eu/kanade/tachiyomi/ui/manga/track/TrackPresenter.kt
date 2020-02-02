package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import kotlinx.coroutines.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


class TrackPresenter(
        val manga: Manga,
        preferences: PreferencesHelper = Injekt.get(),
        private val db: DatabaseHelper = Injekt.get(),
        private val trackManager: TrackManager = Injekt.get()
) : BasePresenter<TrackController>() {

    private var trackList: List<TrackItem> = emptyList()

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged } }

    private val mdex by lazy { Injekt.get<SourceManager>().getMangadex() as HttpSource }

    private var trackSubscription: Subscription? = null

    private var searchSubscription: Subscription? = null

    private var refreshSubscription: Subscription? = null

    private var exceptionHandler = CoroutineExceptionHandler { _, error -> GlobalScope.launch(Dispatchers.Main) { view?.onRefreshError(error) } }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        job = Job()
        job = launch(exceptionHandler)
        { registerMdList(manga) }
    }

    fun fetchTrackings() {
        trackSubscription?.let { remove(it) }
        trackSubscription = db.getTracks(manga)
                .asRxObservable()
                .map { tracks ->
                    loggedServices.map { service ->
                        TrackItem(tracks.find { it.sync_id == service.id }, service)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { trackList = it }
                .subscribeLatestCache(TrackController::onNextTrackings)
    }

    fun refresh() {
        job = Job()
        job = launch(exceptionHandler)
        {
            refreshMdList(trackList[0].track!!)

        }

    }

    private fun refreshOthers() {
        refreshSubscription?.let { remove(it) }

        refreshSubscription = Observable.from(trackList)
                .filter { it.track != null }
                .concatMap { item ->
                    item.service.refresh(item.track!!)
                            .flatMap { db.insertTrack(it).asRxObservable() }
                            .map { item }
                            .onErrorReturn { item }
                }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeFirst({ view, _ -> view.onRefreshDone() },
                        TrackController::onRefreshError)
    }

    private suspend fun refreshMdList(track: Track) {
        withContext(Dispatchers.IO) {
            val remoteTrack = mdex.fetchTrackingInfo(manga)
            track.copyPersonalFrom(remoteTrack)
            track.total_chapters = remoteTrack.total_chapters
            db.insertTrack(track).executeAsBlocking()
        }
        withContext(Dispatchers.Main)
        {
            refreshOthers()
        }
    }

    fun search(query: String, service: TrackService) {
        searchSubscription?.let { remove(it) }
        searchSubscription = service.search(query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(TrackController::onSearchResults,
                        TrackController::onSearchResultsError)
    }

    private suspend fun registerMdList(manga: Manga) {
        withContext(Dispatchers.IO) {
            val count = db.getTracks(manga).executeAsBlocking().filter { it.sync_id == TrackManager.MDLIST }.count()
            if (count == 0) {
                val track = mdex.fetchTrackingInfo(manga)
                track.manga_id = manga.id!!
                db.insertTrack(track).executeAsBlocking()
            }
        }
        withContext(Dispatchers.Main) {
            fetchTrackings()
        }
    }

    fun registerTracking(item: Track?, service: TrackService) {
        if (item != null) {
            item.manga_id = manga.id!!
            add(service.bind(item)
                    .flatMap { db.insertTrack(item).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeFirst({ view, _ -> view.onRefreshDone() },
                            TrackController::onRefreshError))
        } else {
            db.deleteTrackForManga(manga, service).executeAsBlocking()
        }
    }

    private fun updateRemote(track: Track, service: TrackService) {
        if (service.id == TrackManager.MDLIST) {
            job = Job()
            job = launch(exceptionHandler) { updateMdList(track) }
        } else {
            service.update(track)
                    .flatMap { db.insertTrack(track).asRxObservable() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeFirst({ view, _ -> view.onRefreshDone() },
                            { view, error ->
                                view.onRefreshError(error)

                                // Restart on error to set old values
                                fetchTrackings()
                            })
        }
    }

    private suspend fun updateMdList(track: Track) {
        withContext(Dispatchers.IO) {
            mdex.updateReadingProgress(track)
            val followStatus = FollowStatus.fromInt(track.status)!!
            if (manga.follow_status != followStatus) {
                mdex.updateFollowStatus(MdUtil.getMangaId(track.tracking_url), followStatus)
                manga.follow_status = followStatus
                db.insertManga(manga).executeAsBlocking()
            }
            db.insertTrack(track).executeAsBlocking()
        }
        withContext(Dispatchers.Main) {
            view?.onRefreshDone()
            fetchTrackings()
        }
    }

    fun setStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusList()[index]
        updateRemote(track, item.service)
    }

    fun setScore(item: TrackItem, index: Int) {
        val track = item.track!!
        track.score = item.service.indexToScore(index)
        updateRemote(track, item.service)
    }

    fun setLastChapterRead(item: TrackItem, chapterNumber: Int) {
        val track = item.track!!
        track.last_chapter_read = chapterNumber
        updateRemote(track, item.service)
    }

}