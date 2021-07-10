package eu.kanade.tachiyomi

import android.app.Application
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.follows.FollowsSyncService
import eu.kanade.tachiyomi.jobs.tracking.TrackingSyncService
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.ApiMangaParser
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory { PreferencesHelper(app) }

        addSingletonFactory { DatabaseHelper(app) }

        addSingletonFactory { ChapterCache(app) }

        addSingletonFactory { CoverCache(app) }

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory { SourceManager() }

        addSingletonFactory { DownloadManager(app) }

        addSingletonFactory { TrackManager(app) }

        addSingletonFactory { Gson() }

        addSingletonFactory { ChapterFilter() }

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingleton(FilterHandler())

        addSingleton(FollowsHandler())

        addSingleton(MangaHandler())

        addSingleton(ApiMangaParser())

        addSingleton(SearchHandler())

        addSingleton(PageHandler())

        addSingleton(ImageHandler())

        addSingleton(SimilarHandler())

        addSingleton(LatestChapterHandler())

        addSingleton(MangaDexLoginHelper())

        addSingleton(MangaPlusHandler())

        addSingleton(StatusHandler())

        addSingleton(FollowsSyncService())

        addSingleton(TrackingSyncService())

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { ChapterFilter() }

        addSingletonFactory { MangaShortcutManager() }

        // Asynchronously init expensive components for a faster cold start

        GlobalScope.launch { get<PreferencesHelper>() }

        GlobalScope.launch { get<NetworkHelper>() }

        GlobalScope.launch { get<SourceManager>() }

        GlobalScope.launch { get<DatabaseHelper>() }

        GlobalScope.launch { get<DownloadManager>() }
    }
}
