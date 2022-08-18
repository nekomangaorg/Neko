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
    val mangaId: String = "",
    private val repo: SimilarRepository = Injekt.get(),
) : BaseCoroutinePresenter<SimilarController>() {

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _mangaMap = MutableStateFlow(emptyMap<Int, List<DisplayManga>>())
    val mangaMap: StateFlow<Map<Int, List<DisplayManga>>> = _mangaMap.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        presenterScope.launch {
            getSimilarManga()
        }
    }

    fun getSimilarManga(forceRefresh: Boolean = false) {
        presenterScope.launch {
            _isRefreshing.value = true
            _mangaMap.value = emptyMap()
            if (mangaId.isNotEmpty()) {
                val list = repo.fetchSimilar(mangaId, forceRefresh)
                _mangaMap.value = list.associate { it.type to it.manga }
            }
            _isRefreshing.value = false
        }
    }
}
