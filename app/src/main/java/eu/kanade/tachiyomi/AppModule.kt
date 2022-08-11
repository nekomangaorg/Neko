package eu.kanade.tachiyomi

import android.app.Application
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.follows.FollowsSyncService
import eu.kanade.tachiyomi.jobs.migrate.V5MigrationService
import eu.kanade.tachiyomi.jobs.tracking.TrackingSyncService
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.ApiMangaParser
import eu.kanade.tachiyomi.source.online.handlers.ArtworkHandler
import eu.kanade.tachiyomi.source.online.handlers.FilterHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.handlers.external.AzukiHandler
import eu.kanade.tachiyomi.source.online.handlers.external.BilibiliHandler
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaHotHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaPlusHandler
import eu.kanade.tachiyomi.ui.manga.MangaUpdateCoordinator
import eu.kanade.tachiyomi.ui.similar.SimilarRepository
import eu.kanade.tachiyomi.ui.source.latest.LatestRepository
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
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

        addSingletonFactory { ChapterFilter() }

        addSingletonFactory { ChapterItemFilter() }

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { MangaMappings(app.applicationContext) }

        addSingleton(FilterHandler())

        addSingleton(FollowsHandler())

        addSingleton(ArtworkHandler())

        addSingleton(MangaHandler())

        addSingleton(ApiMangaParser())

        addSingleton(SearchHandler())

        addSingleton(PageHandler())

        addSingleton(ImageHandler())

        addSingleton(SimilarHandler())

        addSingleton(LatestChapterHandler())

        addSingleton(MangaDexLoginHelper())

        addSingleton(MangaPlusHandler())

        addSingleton(BilibiliHandler())

        addSingleton(AzukiHandler())

        addSingleton(ComikeyHandler())

        addSingleton(MangaHotHandler())

        addSingleton(StatusHandler())

        addSingleton(FollowsSyncService())

        addSingleton(V5MigrationService())

        addSingleton(TrackingSyncService())

        addSingleton(SimilarRepository())

        addSingleton(MangaUpdateCoordinator())

        addSingleton(LatestRepository())

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { ChapterFilter() }

        addSingletonFactory { MangaShortcutManager() }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<PreferencesHelper>()

            get<NetworkHelper>()

            get<SourceManager>()

            get<DatabaseHelper>()

            get<DownloadManager>()
        }
    }
}
