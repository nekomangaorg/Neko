package eu.kanade.tachiyomi.ui.setting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.CoilDiskCache
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.R
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.UiText
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.injectLazy

class DataStorageSettingsViewModel : ViewModel() {

    val libraryPreferences: LibraryPreferences by injectLazy()

    val storagePreferences: StoragePreferences by injectLazy()

    val applicationContext: Application by injectLazy()

    val chapterCache: ChapterCache by injectLazy()

    val coverCache: CoverCache by injectLazy()

    val network: NetworkHelper by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText.StringResource>()
    val toastEvent = _toastEvent.asSharedFlow()

    private val _cacheData = MutableStateFlow(CacheData())

    val cacheData = _cacheData.asStateFlow()

    init {
        DiskUtil.observeDiskSpace(applicationContext.cacheDir, applicationContext).observeAndUpdate(
            viewModelScope
        ) { newSize ->
            _cacheData.update { it.copy(parentCacheSize = newSize) }
        }

        DiskUtil.observeDiskSpace(chapterCache.cacheDir, applicationContext).observeAndUpdate(
            viewModelScope
        ) { newSize ->
            _cacheData.update { it.copy(chapterDiskCacheSize = newSize) }
        }

        DiskUtil.observeDiskSpace(coverCache.cacheDir, applicationContext).observeAndUpdate(
            viewModelScope
        ) { newSize ->
            _cacheData.update { it.copy(coverCacheSize = newSize) }
        }

        DiskUtil.observeDiskSpace(coverCache.customCoverCacheDir, applicationContext)
            .observeAndUpdate(viewModelScope) { newSize ->
                _cacheData.update { it.copy(customCoverCacheSize = newSize) }
            }
        DiskUtil.observeDiskSpace(coverCache.onlineCoverDirectory, applicationContext)
            .observeAndUpdate(viewModelScope) { newSize ->
                _cacheData.update { it.copy(onlineCoverCacheSize = newSize) }
            }
        DiskUtil.observeDiskSpace(
                File(applicationContext.cacheDir, CoilDiskCache.FOLDER_NAME),
                applicationContext,
            )
            .observeAndUpdate(viewModelScope) { newSize ->
                _cacheData.update { it.copy(imageCacheSize = newSize) }
            }
        DiskUtil.observeDiskSpace(network.cacheDir, applicationContext).observeAndUpdate(
            viewModelScope
        ) { newSize ->
            _cacheData.update { it.copy(networkCacheSize = newSize) }
        }

        DiskUtil.observeDiskSpace(applicationContext.cacheDir, applicationContext, true)
            .observeAndUpdate(viewModelScope) { newSize ->
                _cacheData.update { it.copy(tempFileCacheSize = newSize) }
            }
    }

    fun clearParentCache(cacheType: CacheType) {
        viewModelScope.launchNonCancellable {
            launchIO {
                when (cacheType) {
                    CacheType.Parent ->
                        DiskUtil.cleanupDiskSpace(applicationContext.cacheDir, applicationContext)
                    CacheType.ChapterDisk -> chapterCache.deleteCache()
                    CacheType.Cover -> coverCache.deleteOldCovers()
                    CacheType.CustomCover -> coverCache.deleteAllCustomCachedCovers()
                    CacheType.OnlineCover -> coverCache.deleteAllCachedCovers()
                    CacheType.Image ->
                        DiskUtil.cleanupDiskSpace(
                            File(applicationContext.cacheDir, CoilDiskCache.FOLDER_NAME),
                            applicationContext,
                        )
                    CacheType.Network ->
                        DiskUtil.cleanupDiskSpace(network.cacheDir, applicationContext)
                    CacheType.Temp ->
                        DiskUtil.cleanupDiskSpace(
                            applicationContext.cacheDir,
                            applicationContext,
                            true,
                        )
                }
            }
            _toastEvent.emit(UiText.StringResource(R.string.cache_cleared))
        }
    }
}

enum class CacheType {
    Parent,
    ChapterDisk,
    Cover,
    CustomCover,
    OnlineCover,
    Image,
    Network,
    Temp,
}

data class CacheData(
    val parentCacheSize: String = "",
    val chapterDiskCacheSize: String = "",
    val coverCacheSize: String = "",
    val customCoverCacheSize: String = "",
    val onlineCoverCacheSize: String = "",
    val imageCacheSize: String = "",
    val networkCacheSize: String = "",
    val tempFileCacheSize: String = "",
)
