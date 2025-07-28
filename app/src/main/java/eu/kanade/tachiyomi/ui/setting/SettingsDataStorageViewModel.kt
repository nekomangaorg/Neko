package eu.kanade.tachiyomi.ui.setting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.image.coil.CoilDiskCache
import eu.kanade.tachiyomi.network.NetworkHelper
import java.io.File
import kotlin.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.nekomanga.domain.library.LibraryPreferences
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsDataStorageViewModel : ViewModel() {

    val libraryPreferences by injectLazy<LibraryPreferences>()

    val applicationContext by injectLazy<Application>()

    val chapterCache by injectLazy<ChapterCache>()

    val coverCache by injectLazy<CoverCache>()

    val network by injectLazy<NetworkHelper>()

    val db by injectLazy<DatabaseHelper>()

    private val _cacheData = MutableStateFlow(CacheData())

    val cacheData = _cacheData.asStateFlow()

    init {
        DiskUtil.observeDiskSpace(applicationContext.cacheDir, applicationContext)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(parentCacheSize = newSize) } }
            .launchIn(viewModelScope)

        DiskUtil.observeDiskSpace(chapterCache.cacheDir, applicationContext)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(chapterDiskCacheSize = newSize) } }
            .launchIn(viewModelScope)

        DiskUtil.observeDiskSpace(coverCache.cacheDir, applicationContext)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(coverCacheSize = newSize) } }
            .launchIn(viewModelScope)

        DiskUtil.observeDiskSpace(coverCache.customCoverCacheDir, applicationContext)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(customCoverCacheSize = newSize) } }
            .launchIn(viewModelScope)
        DiskUtil.observeDiskSpace(coverCache.onlineCoverDirectory, applicationContext)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(onlineCoverCacheSize = newSize) } }
            .launchIn(viewModelScope)
        DiskUtil.observeDiskSpace(
                File(applicationContext.cacheDir, CoilDiskCache.FOLDER_NAME),
                applicationContext,
            )
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(imageCacheSize = newSize) } }
            .launchIn(viewModelScope)
        DiskUtil.observeDiskSpace(network.cacheDir, applicationContext)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(networkCacheSize = newSize) } }
            .launchIn(viewModelScope)

        DiskUtil.observeDiskSpace(applicationContext.cacheDir, applicationContext, true)
            .distinctUntilChanged()
            .onEach { newSize -> _cacheData.update { it.copy(tempFileCacheSize = newSize) } }
            .launchIn(viewModelScope)

        /*

        Text(text = "Image cache: ${DiskUtil.readableDiskSize(context, CoilDiskCache.get(context).size)}")
        Text(text = "Network cache: ${DiskUtil.readableDiskSize(context, network.cacheDir)}")
        Text(text = "Temp file cache: ${DiskUtil.readableDiskSize(context, DiskUtil.getCacheDirSize(context))}")*/
    }
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
