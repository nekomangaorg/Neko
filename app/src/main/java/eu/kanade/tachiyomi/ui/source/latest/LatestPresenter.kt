package eu.kanade.tachiyomi.ui.source.latest

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import kotlinx.coroutines.flow.Flow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LatestPresenter(
    private val latestRepository: LatestRepository = Injekt.get(),
) : BaseCoroutinePresenter<LatestController>() {

    lateinit var mangaList: Flow<PagingData<DisplayManga>>

    override fun onCreate() {
        super.onCreate()
        mangaList = Pager(PagingConfig(pageSize = 20)) {
            LatestPagingSource(latestRepository)
        }.flow.cachedIn(presenterScope)
    }
}
