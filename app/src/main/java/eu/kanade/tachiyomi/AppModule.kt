package eu.kanade.tachiyomi

import android.app.Application
import com.google.gson.Gson
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.CustomMangaManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    @OptIn(UnstableDefault::class)
    override fun InjektRegistrar.registerInjectables() {

        addSingleton(app)

        addSingletonFactory { PreferencesHelper(app) }

        addSingletonFactory { DatabaseHelper(app) }

        addSingletonFactory { ChapterCache(app) }

        addSingletonFactory { CoverCache(app) }

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory {
            val config = JsonConfiguration(
                isLenient = true, ignoreUnknownKeys = true, serializeSpecialFloatingPointValues = true,
                useArrayPolymorphism = true, prettyPrint = true
            )
            Json(config)
        }

        addSingletonFactory { SourceManager() }

        addSingletonFactory { DownloadManager(app) }

        addSingletonFactory { CustomMangaManager(app) }

        addSingletonFactory { TrackManager(app) }

        addSingletonFactory { Gson() }

        // Asynchronously init expensive components for a faster cold start

        GlobalScope.launch { get<PreferencesHelper>() }

        GlobalScope.launch { get<NetworkHelper>() }

        GlobalScope.launch { get<SourceManager>() }

        GlobalScope.launch { get<DatabaseHelper>() }

        GlobalScope.launch { get<DownloadManager>() }

        GlobalScope.launch { get<CustomMangaManager>() }
    }
}
