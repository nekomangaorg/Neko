package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.library.LibraryUpdateRanker.Companion.relevanceRanking
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Companion.start
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.isServiceRunning
import eu.kanade.tachiyomi.util.notification
import eu.kanade.tachiyomi.util.notificationManager
import eu.kanade.tachiyomi.util.syncChaptersWithSource
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class will take care of updating the chapters of the manga from the library. It can be
 * started calling the [start] method. If it's already running, it won't do anything.
 * While the library is updating, a [PowerManager.WakeLock] will be held until the update is
 * completed, preventing the device from going to sleep mode. A notification will display the
 * progress of the update, and if case of an unexpected error, this service will be silently
 * destroyed.
 */
class LibraryUpdateService(
    val db: DatabaseHelper = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    val trackManager: TrackManager = Injekt.get(),
    val coverCache: CoverCache = Injekt.get()
) : Service() {

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    /**
     * Subscription where the update is done.
     */
    private var subscription: Subscription? = null

    var job: Job? = null

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelIntent by lazy {
        NotificationReceiver.cancelLibraryUpdatePendingBroadcast(this)
    }
    val notificationBitmap by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    private val progressNotification by lazy {
        NotificationCompat.Builder(this, Notifications.CHANNEL_LIBRARY)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_refresh_grey)
            .setLargeIcon(notificationBitmap)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .addAction(R.drawable.ic_clear_grey, getString(android.R.string.cancel), cancelIntent)
    }

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Manga meta data and  chapters
        SYNC_FOLLOWS, // Manga in reading, rereading
        TRACKING // Tracking metadata
    }

    companion object {

        /**
         * Key for category to update.
         */
        const val KEY_CATEGORY = "category"

        /**
         * Key that defines what should be updated.
         */
        const val KEY_TARGET = "target"

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(LibraryUpdateService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         * @param category a specific category to update, or null for global update.
         * @param target defines what should be updated.
         */
        fun start(context: Context, category: Category? = null, target: Target = Target.CHAPTERS) {
            if (!isRunning(context)) {
                val intent = Intent(context, LibraryUpdateService::class.java).apply {
                    putExtra(KEY_TARGET, target)
                    category?.let { putExtra(KEY_CATEGORY, it.id) }
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(intent)
                } else {
                    context.startForegroundService(intent)
                }
            }
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, LibraryUpdateService::class.java))
        }
    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_LIBRARY_PROGRESS, progressNotification.build())
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "LibraryUpdateService:WakeLock"
        )
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(30))
    }

    /**
     * Method called when the service is destroyed. It destroys subscriptions and releases the wake
     * lock.
     */
    override fun onDestroy() {
        job?.cancel()
        subscription?.unsubscribe()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Method called when the service receives an intent.
     *
     * @param intent the start intent from.
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return Service.START_NOT_STICKY
        val target = intent.getSerializableExtra(KEY_TARGET) as? Target
            ?: return Service.START_NOT_STICKY

        // Unsubscribe from any previous subscription if needed.
        subscription?.unsubscribe()

        // Update favorite manga. Destroy service when completed or in case of an error.
        val mangaList = getMangaToUpdate(intent, target)
            .sortedWith(relevanceRanking())

        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception)
            stopSelf(startId)
        }
        // Update either chapter list or manga details.
        if (target == Target.SYNC_FOLLOWS) {
            job = GlobalScope.launch(handler) {
                syncFollows()
            }
            job?.invokeOnCompletion { stopSelf(startId) }
        } else if (target == Target.CHAPTERS) {
            job = GlobalScope.launch(handler) {
                updateManga(mangaList)
            }
            job?.invokeOnCompletion { stopSelf(startId) }
        } else {
            subscription = Observable
                .defer {
                    updateTrackings(mangaList)
                }.subscribeOn(Schedulers.io())
                .subscribe({
                }, {
                    Timber.e(it)
                    stopSelf(startId)
                }, {
                    stopSelf(startId)
                })
        }
        return Service.START_REDELIVER_INTENT
    }

    /**
     * Returns the list of manga to be updated.
     *
     * @param intent the update intent.
     * @param target the target to update.
     * @return a list of manga to update
     */
    fun getMangaToUpdate(intent: Intent, target: Target): List<LibraryManga> {
        val categoryId = intent.getIntExtra(KEY_CATEGORY, -1)

        var listToUpdate = if (categoryId != -1)
            db.getLibraryMangas().executeAsBlocking().filter { it.category == categoryId }
        else {
            val categoriesToUpdate =
                preferences.libraryUpdateCategories().getOrDefault().map(String::toInt)
            if (categoriesToUpdate.isNotEmpty())
                db.getLibraryMangas().executeAsBlocking()
                    .filter { it.category in categoriesToUpdate }
                    .distinctBy { it.id }
            else
                db.getLibraryMangas().executeAsBlocking().distinctBy { it.id }
        }

        if (target == Target.CHAPTERS && preferences.updateOnlyNonCompleted()) {
            listToUpdate = listToUpdate.filter { it.status != SManga.COMPLETED }
        }

        return listToUpdate
    }

    /**
     * Method that updates the given list of manga. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each manga it calls [updateManga] and updates the notification showing the current
     * progress.
     *
     * @param mangaToUpdate the list to update
     */
    private suspend fun updateManga(mangaToUpdate: List<LibraryManga>) {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)
        // List containing new updates
        val newUpdates = ArrayList<Pair<Manga, Array<Chapter>>>()
        // list containing failed updates
        val failedUpdates = ArrayList<Manga>()
        // List containing categories that get included in downloads.
        val categoriesToDownload =
            preferences.downloadNewCategories().getOrDefault().map(String::toInt)
        // Boolean to determine if user wants to automatically download new chapters.
        val downloadNew = preferences.downloadNew().getOrDefault()
        // Boolean to determine if DownloadManager has downloads
        var hasDownloads = false

        mangaToUpdate.forEach {
            try {

                showProgressNotification(it, count.andIncrement, mangaToUpdate.size)
                val pair = updateManga(it)

                // If pair first is empty there are no new manga chapters
                if (pair.first.isNotEmpty()) {

                    if (downloadNew && (categoriesToDownload.isEmpty() ||
                            it.category in categoriesToDownload)
                    ) {

                        downloadChapters(it, pair.first)
                        hasDownloads = true
                    }

                    // Convert to the manga that contains new chapters.
                    val mangaWithNewChapters = Pair(
                        it,
                        (pair.first.sortedByDescending { ch -> ch.source_order }.toTypedArray())
                    )
                    newUpdates.add(mangaWithNewChapters)
                }
            } catch (e: Exception) {
                Timber.e(e)
                failedUpdates.add(it)
            }
        }

        if (newUpdates.isNotEmpty()) {
            showResultNotification(newUpdates)
            if (downloadNew && hasDownloads) {
                DownloadService.start(this)
            }
        }

        if (failedUpdates.isNotEmpty()) {
            Timber.e("Failed updating: ${failedUpdates.map { it.title }}")
        }

        cancelProgressNotification()
    }

    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // we need to get the chapters from the db so we have chapter ids
        val mangaChapters = db.getChapters(manga).executeAsBlocking()
        val dbChapters = chapters.map {
            mangaChapters.find { mangaChapter -> mangaChapter.url == it.url }!!
        }
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadChapters(manga, dbChapters, false)
    }

    /**
     * Updates the chapters for the given manga and adds them to the database.
     *
     * @param manga the manga to update.
     * @return a pair of the inserted and removed chapters.
     */
    suspend fun updateManga(manga: Manga): Pair<List<Chapter>, List<Chapter>> {
        val source = sourceManager.getMangadex()
        val details = source.fetchMangaAndChapterDetails(manga)

        // delete cover cache image if the thumbnail from network is not empty
        if (!details.first.thumbnail_url.isNullOrEmpty()) {
            coverCache.deleteFromCache(manga.thumbnail_url)
        }
        manga.copyFrom(details.first)
        db.insertManga(manga).executeAsBlocking()
        return syncChaptersWithSource(db, details.second, manga, source)
    }

    /**
     * Method that updates the syncs reading and rereading manga into neko library
     */
    private suspend fun syncFollows() {
        val count = AtomicInteger(0)
        val listManga = sourceManager.getMangadex().fetchAllFollows()
        // filter all follows from Mangadex and only add reading or rereading manga to library
        listManga.filter { it ->
                it.follow_status == FollowStatus.RE_READING || it.follow_status == FollowStatus.READING
            }
            .forEach { networkManga ->
                showProgressNotification(networkManga, count.andIncrement, listManga.size)

                var dbManga = db.getManga(networkManga.url, sourceManager.getMangadex().id)
                    .executeAsBlocking()
                if (dbManga == null) {
                    dbManga = Manga.create(
                        networkManga.url,
                        networkManga.title,
                        sourceManager.getMangadex().id
                    )
                }

                dbManga.copyFrom(networkManga)
                dbManga.favorite = true
                db.insertManga(dbManga).executeAsBlocking()
            }
        cancelProgressNotification()
    }

    /**
     * Method that updates the metadata of the connected tracking services. It's called in a
     * background thread, so it's safe to do heavy operations or network calls here.
     */
    private fun updateTrackings(mangaToUpdate: List<LibraryManga>): Observable<LibraryManga> {
        // Initialize the variables holding the progress of the updates.
        var count = 0

        val loggedServices = trackManager.services.filter { it.isLogged }

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
            // Notify manga that will update.
            .doOnNext { showProgressNotification(it, count++, mangaToUpdate.size) }
            // Update the tracking details.
            .concatMap { manga ->
                val tracks = db.getTracks(manga).executeAsBlocking()

                Observable.from(tracks)
                    .concatMap { track ->
                        val service = trackManager.getService(track.sync_id)
                        if (service != null && service in loggedServices) {
                            service.refresh(track)
                                .doOnNext { db.insertTrack(it).executeAsBlocking() }
                                .onErrorReturn { track }
                        } else {
                            Observable.empty()
                        }
                    }
                    .map { manga }
            }
            .doOnCompleted {
                cancelProgressNotification()
            }
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(manga: SManga, current: Int, total: Int) {
        notificationManager.notify(
            Notifications.ID_LIBRARY_PROGRESS, progressNotification
                .setContentTitle(manga.title)
                .setProgress(total, current, false)
                .build()
        )
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of manga with new updates.
     */
    private fun showResultNotification(updates: List<Pair<Manga, Array<Chapter>>>) {
        val notifications = ArrayList<Pair<Notification, Int>>()
        updates.forEach {
            val manga = it.first
            val chapters = it.second
            val chapterNames = chapters.map { chapter -> chapter.name }.toSet()
            notifications.add(Pair(notification(Notifications.CHANNEL_NEW_CHAPTERS) {
                setSmallIcon(R.drawable.ic_neko_notification)
                try {
                    val icon = GlideApp.with(this@LibraryUpdateService)
                        .asBitmap().load(manga).dontTransform().centerCrop().circleCrop()
                        .override(256, 256).submit().get()
                    setLargeIcon(icon)
                } catch (e: Exception) {
                } catch (e: Exception) {
                }
                setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                setContentTitle(manga.title)
                color = ContextCompat.getColor(this@LibraryUpdateService, R.color.colorPrimary)
                val chaptersNames = if (chapterNames.size > 5) {
                    "${chapterNames.take(4).joinToString("\n")}\n" +
                        getString(R.string.notification_and_n_more, (chapterNames.size - 4))
                } else chapterNames.joinToString("\n ")
                setContentText(chaptersNames)
                setStyle(NotificationCompat.BigTextStyle().bigText(chaptersNames))
                priority = NotificationCompat.PRIORITY_HIGH
                setGroup(Notifications.GROUP_NEW_CHAPTERS)
                setContentIntent(
                    NotificationReceiver.openChapterPendingActivity(
                        this@LibraryUpdateService,
                        manga,
                        chapters.first()
                    )
                )
                addAction(
                    R.drawable.ic_list_grey, getString(R.string.action_view_chapters),
                    NotificationReceiver.openChapterPendingActivity(
                        this@LibraryUpdateService,
                        manga, Notifications.ID_NEW_CHAPTERS
                    )
                )
                setAutoCancel(true)
            }, manga.id.hashCode()))
        }

        NotificationManagerCompat.from(this).apply {
            notifications.forEach {
                notify(it.second, it.first)
            }

            notify(Notifications.ID_NEW_CHAPTERS, notification(Notifications.CHANNEL_NEW_CHAPTERS) {
                setSmallIcon(R.drawable.ic_neko_notification)
                setLargeIcon(notificationBitmap)
                setContentTitle(getString(R.string.notification_new_chapters))
                color = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
                if (updates.size > 1) {
                    setContentText(getString(R.string.notification_new_chapters_text, updates.size))
                    setStyle(NotificationCompat.BigTextStyle().bigText(updates.joinToString("\n") {
                        it.first.title.chop(45)
                    }))
                } else {
                    setContentText(updates.first().first.title.chop(45))
                }
                priority = NotificationCompat.PRIORITY_HIGH
                setGroup(Notifications.GROUP_NEW_CHAPTERS)
                setGroupSummary(true)
                setContentIntent(getNotificationIntent())
                setAutoCancel(true)
            })
        }
    }

    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        notificationManager.cancel(Notifications.ID_LIBRARY_PROGRESS)
    }

    /**
     * Returns an intent to open the main activity.
     */
    private fun getNotificationIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.action = MainActivity.SHORTCUT_RECENTLY_UPDATED
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
