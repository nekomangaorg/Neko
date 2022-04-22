package eu.kanade.tachiyomi.ui.manga.similar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.similar.SimilarController
import eu.kanade.tachiyomi.ui.similar.SimilarRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [SimilarController]
 */
class SimilarPresenter(
    val mangaId: String = "",
    private val repo: SimilarRepository = Injekt.get(),
) : BaseCoroutinePresenter<SimilarController>() {

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _mangaMap = MutableLiveData(emptyMap<Int, List<DisplayManga>>())
    val mangaMap: LiveData<Map<Int, List<DisplayManga>>> = _mangaMap

    suspend fun getSimilarManga(forceRefresh: Boolean = false) {
        _isRefreshing.value = true
        _mangaMap.value = emptyMap()
        if (mangaId.isNotEmpty()) {
            val list = repo.fetchSimilar(mangaId, forceRefresh)
            _mangaMap.value = list.associate { it.type to it.manga }
        }
        _isRefreshing.value = false
    }
}
