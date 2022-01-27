package eu.kanade.tachiyomi.ui.manga.similar

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.similar.SimilarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [SimilarController]. Inherit BrowseSourcePresenter.
 */
class SimilarPresenter(
    val mangaId: Long = 0L,
    val preferences: PreferencesHelper = Injekt.get(),
    val repo: SimilarRepository = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val app: Application = Injekt.get(),
) : BaseCoroutinePresenter() {

    val manga: Manga = db.getManga(mangaId).executeAsBlocking()!!

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _mangaMap = MutableLiveData(emptyMap<String, List<Manga>>())
    val mangaMap: LiveData<Map<String, List<Manga>>> = _mangaMap

    var scope = CoroutineScope(Job() + Dispatchers.Default)

    suspend fun getSimilarManga(forceRefresh: Boolean = false) {
        _isRefreshing.value = true
        _mangaMap.value = emptyMap()
        val list = repo.fetchSimilar(manga, forceRefresh)
        val groupedManga =
            list.map { app.applicationContext.getString(it.type) to it.manga }.toMap()

        _mangaMap.value = groupedManga
        _isRefreshing.value = false
    }
}
