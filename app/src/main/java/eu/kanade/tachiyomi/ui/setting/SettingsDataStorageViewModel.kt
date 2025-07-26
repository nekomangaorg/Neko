package eu.kanade.tachiyomi.ui.setting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import kotlin.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.domain.library.LibraryPreferences
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsDataStorageViewModel : ViewModel() {

    val libraryPreferences by injectLazy<LibraryPreferences>()

    val applicationContext by injectLazy<Application>()

    val db by injectLazy<DatabaseHelper>()

    private val _cacheData = MutableStateFlow(CacheData())

    val cacheData = _cacheData.asStateFlow()

    init {
        viewModelScope.launch {
            DiskUtil.observeDiskSpace(applicationContext.cacheDir)
                .distinctUntilChanged()
                .onEach { newSize -> _cacheData.update { it.copy(parentCacheSize = newSize) } }
                .launchIn(viewModelScope)
        }
    }
}

data class CacheData(
    val parentCacheSize: String = "",
    val chapterDiskCacheSize: String = "",
    val coverCacheSize: String = "",
    val onlineCoverCacheSize: String = "",
    val imageCacheSize: String = "",
    val networkCacheSize: String = "",
    val tempFileCacheSize: String = "",
)
