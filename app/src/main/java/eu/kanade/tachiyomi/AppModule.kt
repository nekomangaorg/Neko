package eu.kanade.tachiyomi

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.follows.FollowsSyncProcessor
import eu.kanade.tachiyomi.jobs.tracking.TrackSyncProcessor
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.ApiMangaParser
import eu.kanade.tachiyomi.source.online.handlers.ArtworkHandler
import eu.kanade.tachiyomi.source.online.handlers.FeedUpdatesHandler
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.ImageHandler
import eu.kanade.tachiyomi.source.online.handlers.LatestChapterHandler
import eu.kanade.tachiyomi.source.online.handlers.ListHandler
import eu.kanade.tachiyomi.source.online.handlers.MangaHandler
import eu.kanade.tachiyomi.source.online.handlers.PageHandler
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.handlers.external.AzukiHandler
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaHotHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.handlers.external.NamiComiHandler
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import eu.kanade.tachiyomi.ui.manga.MangaUpdateCoordinator
import eu.kanade.tachiyomi.ui.source.browse.BrowseRepository
import eu.kanade.tachiyomi.ui.source.latest.DisplayRepository
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.serialization.json.Json
import org.nekomanga.BuildConfig
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.migration.DatabaseMigrations
import org.nekomanga.data.database.repository.ArtworkRepository
import org.nekomanga.data.database.repository.ArtworkRepositoryImpl
import org.nekomanga.data.database.repository.BrowseFilterRepository
import org.nekomanga.data.database.repository.BrowseFilterRepositoryImpl
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.ChapterRepositoryImpl
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.HistoryRepositoryImpl
import org.nekomanga.data.database.repository.MangaAggregateRepository
import org.nekomanga.data.database.repository.MangaAggregateRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.data.database.repository.MergeMangaRepository
import org.nekomanga.data.database.repository.MergeMangaRepositoryImpl
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.ScanlatorGroupRepositoryImpl
import org.nekomanga.data.database.repository.SimilarRepository
import org.nekomanga.data.database.repository.SimilarRepositoryImpl
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.data.database.repository.TrackRepositoryImpl
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.data.database.repository.UploaderRepositoryImpl
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.domain.track.store.DelayedTrackingStore
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.screens.feed.FeedRepository
import org.nekomanga.presentation.screens.similar.SimilarRepo
import org.nekomanga.usecases.chapters.CalculateChapterFilterUseCase
import org.nekomanga.usecases.chapters.ChapterUseCases
import org.nekomanga.usecases.library.FilterLibraryMangaUseCase
import org.nekomanga.usecases.manga.MangaUseCases
import org.nekomanga.usecases.preferences.GetDateFormatUseCase
import org.nekomanga.usecases.preferences.GetFormattedBuildTimeUseCase
import org.nekomanga.usecases.tracking.TrackUseCases
import tachiyomi.core.preference.AndroidPreferenceStore
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.util.storage.AndroidStorageFolderProvider
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory {
            Room.databaseBuilder(app, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(DatabaseMigrations.MIGRATION_45_46)
                .fallbackToDestructiveMigration(false)
                .build()
        }

        addSingletonFactory<ArtworkRepository> {
            ArtworkRepositoryImpl(artworkDao = get<AppDatabase>().artworkDao())
        }

        addSingletonFactory<BrowseFilterRepository> {
            BrowseFilterRepositoryImpl(browseFilterDao = get<AppDatabase>().browseFilterDao())
        }

        addSingletonFactory<CategoryRepository> {
            CategoryRepositoryImpl(
                categoryDao = get<AppDatabase>().categoryDao(),
                mangaCategoryDao = get<AppDatabase>().mangaCategoryDao(),
            )
        }

        addSingletonFactory<ChapterRepository> {
            ChapterRepositoryImpl(chapterDao = get<AppDatabase>().chapterDao())
        }

        addSingletonFactory<HistoryRepository> {
            HistoryRepositoryImpl(historyDao = get<AppDatabase>().historyDao())
        }

        addSingletonFactory<MangaRepository> {
            MangaRepositoryImpl(
                libraryDao = get<AppDatabase>().libraryDao(),
                mangaDao = get<AppDatabase>().mangaDao(), // INJECTED HERE
                mangaDexPreferences = get(),
                libraryPreferences = get(),
            )
        }

        addSingletonFactory<MergeMangaRepository> {
            MergeMangaRepositoryImpl(mergeMangaDao = get<AppDatabase>().mergeMangaDao())
        }

        // Bind the ScanlatorGroup repository
        addSingletonFactory<ScanlatorGroupRepository> {
            ScanlatorGroupRepositoryImpl(scanlatorGroupDao = get<AppDatabase>().scanlatorGroupDao())
        }

        // Bind the Similar repository
        addSingletonFactory<SimilarRepository> {
            SimilarRepositoryImpl(similarDao = get<AppDatabase>().similarDao())
        }

        addSingletonFactory<MangaAggregateRepository> {
            MangaAggregateRepositoryImpl(mangaAggregateDao = get<AppDatabase>().mangaAggregateDao())
        }

        // Bind the Uploader repository
        addSingletonFactory<UploaderRepository> {
            UploaderRepositoryImpl(uploaderDao = get<AppDatabase>().uploaderDao())
        }

        // Bind the Track repository
        addSingletonFactory<TrackRepository> {
            TrackRepositoryImpl(trackDao = get<AppDatabase>().trackDao())
        }

        addSingletonFactory { ChapterCache(app) }

        addSingletonFactory { CoverCache(app) }

        addSingletonFactory { WorkManager.getInstance(app) }

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory { NetworkServices() }

        addSingletonFactory { SourceManager() }

        addSingletonFactory { DownloadManager(app) }

        addSingletonFactory { TrackManager(app) }

        addSingletonFactory { ChapterItemFilter() }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        }

        addSingletonFactory { MangaMappings(app.applicationContext) }

        addSingletonFactory { StorageManager(app, get()) }

        addSingleton(FollowsHandler())

        addSingleton(ArtworkHandler())

        addSingleton(MangaHandler())

        addSingleton(ApiMangaParser())

        addSingleton(SearchHandler())

        addSingleton(ListHandler())

        addSingleton(PageHandler())

        addSingleton(ImageHandler())

        addSingleton(SimilarHandler())

        addSingleton(LatestChapterHandler())

        addSingleton(FeedUpdatesHandler())

        addSingleton(MangaDexLoginHelper())

        addSingleton(MangaPlusHandler())

        addSingleton(NamiComiHandler())

        addSingleton(AzukiHandler())

        addSingleton(ComikeyHandler())

        addSingleton(MangaHotHandler())

        addSingleton(StatusHandler())

        addSingleton(FollowsSyncProcessor())

        addSingleton(TrackSyncProcessor())

        addSingleton(DelayedTrackingStore(app))

        addSingleton(SimilarRepo())

        addSingleton(MangaUpdateCoordinator())

        addSingleton(DisplayRepository())

        addSingleton(BrowseRepository())

        addSingleton(CalculateChapterFilterUseCase())
        addSingleton(ChapterUseCases())

        addSingleton(FilterLibraryMangaUseCase())

        addSingleton(MangaUseCases())

        addSingleton(TrackUseCases())

        addSingleton(FeedRepository())

        addSingleton(AppSnackbarManager())

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { MangaShortcutManager() }

        // Asynchronously init expensive components for a faster cold start
        launchIO {
            try {
                get<NetworkHelper>()

                get<SourceManager>()

                get<AppDatabase>()

                get<DownloadManager>()
            } catch (e: Exception) {
                TimberKt.e(e) { "Failed to initialize components" }
            }
        }
    }
}

class PreferenceModule(val application: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> { AndroidPreferenceStore(application) }

        addSingletonFactory { DownloadProvider(get()) }

        addSingletonFactory { AndroidStorageFolderProvider(application) }

        addSingletonFactory {
            StoragePreferences(
                context = application,
                folderProvider = get<AndroidStorageFolderProvider>(),
                preferenceStore = get(),
            )
        }

        addSingletonFactory { SecurityPreferences(get()) }

        addSingletonFactory { LibraryPreferences(get()) }

        addSingletonFactory { ReaderPreferences(get()) }

        addSingletonFactory { NetworkPreferences(get(), BuildConfig.DEBUG) }

        addSingletonFactory { MangaDetailsPreferences(get()) }

        addSingletonFactory { MangaDexPreferences(get()) }

        addSingletonFactory { PreferencesHelper(context = application, preferenceStore = get()) }
        addSingletonFactory { GetDateFormatUseCase(get<PreferencesHelper>()) }
        addSingletonFactory { GetFormattedBuildTimeUseCase(get()) }
    }
}
