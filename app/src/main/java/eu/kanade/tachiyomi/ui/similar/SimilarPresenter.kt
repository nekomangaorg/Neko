package eu.kanade.tachiyomi.ui.manga.similar

import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.similar.SimilarController
import eu.kanade.tachiyomi.ui.similar.SimilarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [SimilarController]
 */
class SimilarPresenter(
    private val mangaUUID: String,
    private val repo: SimilarRepository = Injekt.get(),
) : BaseCoroutinePresenter<SimilarController>() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _mangaMap = MutableStateFlow(emptyMap<Int, List<DisplayManga>>())
    val mangaMap: StateFlow<Map<Int, List<DisplayManga>>> = _mangaMap.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        getSimilarManga()
    }

    fun refresh() {
        getSimilarManga(true)
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        presenterScope.launch {
            _isRefreshing.value = true
            _mangaMap.value = emptyMap()
            if (mangaUUID.isNotEmpty()) {
                val list = repo.fetchSimilar(mangaUUID, forceRefresh)
                _mangaMap.value = list.associate { it.type to it.manga }
            }
            _isRefreshing.value = false
        }
    }
}
