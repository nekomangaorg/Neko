package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.launchUI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nekomanga.R
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.injectLazy

// This class just holds some injects.  If a settings screen requires
class AdvancedSettingsViewModel : ViewModel() {
    val preferences: PreferencesHelper by injectLazy()

    val readerPreferences: ReaderPreferences by injectLazy()

    val mangaDetailsPreferences: MangaDetailsPreferences by injectLazy()
    val networkPreference: NetworkPreferences by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()

    val downloadManager: DownloadManager by injectLazy()

    val historyRepository: HistoryRepository by injectLazy()

    val categoryRepository: CategoryRepository by injectLazy()

    val chapterRepository: ChapterRepository by injectLazy()

    val mangaRepository: MangaRepository by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun clearNetworkCookies() {
        viewModelScope.launchUI {
            networkHelper.cookieManager.removeAll()
            _toastEvent.emit(UiText.StringResource(R.string.cookies_cleared))
        }
    }

    fun cleanupDownloads(removeRead: Boolean, removeNonFavorite: Boolean) {
        viewModelScope.launchNonCancellable {
            launchIO {
                _toastEvent.emit(UiText.StringResource(R.string.starting_cleanup))
                var foldersCleared = 0
                val mangaMap =
                    mangaRepository.getMangaList().associateBy {
                        downloadManager.getMangaDirName(it)
                    }
                val mangaFolders = downloadManager.getMangaFolders()

                val chaptersByMangaId =
                    mangaFolders
                        .mapNotNull { mangaMap[it.name]?.id }
                        .chunked(900)
                        .map { chunk -> async { chapterRepository.getChaptersForMangaIds(chunk) } }
                        .awaitAll()
                        .flatten()
                        .groupBy { it.manga_id }

                for (mangaFolder in mangaFolders) {
                    val manga = mangaMap[mangaFolder.name]
                    if (manga == null) {
                        // download is orphaned delete it if remove non favorited is enabled
                        if (removeNonFavorite) {
                            foldersCleared += 1 + (mangaFolder.listFiles()?.size ?: 0)
                            mangaFolder.delete()
                        }
                        continue
                    }
                    val chapterList = chaptersByMangaId[manga.id] ?: emptyList()
                    foldersCleared +=
                        downloadManager.cleanupChapters(
                            chapterList,
                            manga,
                            removeRead,
                            removeNonFavorite,
                        )
                }
                val stringResource =
                    if (foldersCleared == 0) {
                        UiText.StringResource(R.string.no_folders_to_cleanup)
                    } else {
                        UiText.PluralsResource(
                            R.plurals.cleanup_done,
                            foldersCleared,
                            foldersCleared,
                        )
                    }

                _toastEvent.emit(stringResource)
            }
        }
    }

    fun clearDatabase(keepRead: Boolean) {
        viewModelScope.launchUI {
            mangaDetailsPreferences.coverVibrantColors().delete()
            if (keepRead) {
                mangaRepository.deleteAllNotInLibraryAndNotRead()
            } else {
                mangaRepository.deleteAllNotInLibrary()
            }
            historyRepository.deleteHistoryNoLastRead()

            _toastEvent.emit(UiText.StringResource(R.string.clear_database_completed))
        }
    }

    fun reindexDownloads() {
        viewModelScope.launchNonCancellable {
            launchIO {
                _toastEvent.emit(UiText.StringResource(R.string.reindex_downloads_invalidate))
                downloadManager.refreshCache()
                _toastEvent.emit(UiText.StringResource(R.string.reindex_downloads_complete))
            }
        }
    }

    fun dedupeCategories() {
        viewModelScope.launchNonCancellable {
            launchIO {
                val categories = categoryRepository.getCategories()

                val categoriesByName = categories.groupBy { it.name }

                var duplicates = 0
                for ((_, categories) in categoriesByName) {
                    if (categories.size > 1) {
                        val oldest = categories.minBy { it.id!! }
                        val others = categories.filter { it.id != oldest.id }
                        val mangaCategoryToMove = others.flatMap {
                            categoryRepository.getMangaCategoriesForCategory(it.id!!)
                        }
                        if (mangaCategoryToMove.isNotEmpty()) {
                            mangaCategoryToMove.forEach {
                                it.category_id = oldest.id!!
                                categoryRepository.insertMangaCategory(it)
                            }
                        }
                        categoryRepository.deleteCategories(others)

                        duplicates++
                    }
                }
                val stringResource =
                    if (duplicates == 0) {
                        UiText.StringResource(R.string.no_duplicate_categories)
                    } else {
                        UiText.PluralsResource(
                            R.plurals.category_duplicates_removed,
                            duplicates,
                            duplicates,
                        )
                    }
                _toastEvent.emit(stringResource)
            }
        }
    }
}
