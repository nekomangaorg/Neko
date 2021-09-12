package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crazylegend.activity.getStatusBarHeight
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.composethemeadapter.MdcTheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.SimilarControllerBinding
import eu.kanade.tachiyomi.ui.base.MangaListWithHeader
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.manga.similar.SimilarPresenter
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.util.system.pxToDp
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.launch

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class SimilarController(bundle: Bundle? = null) :
    BaseCoroutineController<SimilarControllerBinding, SimilarPresenter>(bundle) {

    constructor(manga: Manga) : this(
        Bundle().apply {
            putLong(BrowseSourceController.MANGA_ID, manga.id!!)
        }
    )

    override var presenter = SimilarPresenter(bundle!!.getLong(BrowseSourceController.MANGA_ID))

    override fun getTitle(): String? {
        return view?.context?.getString(R.string.similar)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        viewScope.launch {
            presenter.getSimilarManga()
        }

        binding.holder.setContent {
            MdcTheme {
                val refreshing = presenter.isRefreshing.observeAsState(initial = true)
                SwipeRefresh(
                    state = rememberSwipeRefreshState(refreshing.value),
                    modifier = Modifier.padding(top = (activity!!.getStatusBarHeight.pxToDp + 4).dp),
                    onRefresh = {
                        viewScope.launch {
                            presenter.getSimilarManga(true)
                        }
                    },
                    indicator = { state, trigger ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = trigger,
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary
                        )
                    }
                ) {

                    val groupedManga: Map<String, List<Manga>> by presenter.mangaMap.observeAsState(
                        emptyMap())
                    if (groupedManga.isEmpty() && refreshing.value.not()) {

                        //show empty view
                    } else {
                        MangaListWithHeader(groupedManga = groupedManga,
                            modifier = Modifier.padding(top = 10.dp)) { manga ->
                            router.pushController(MangaDetailsController(manga,
                                true).withFadeTransaction())
                        }
                    }
                }
            }
        }
    }

    override fun createBinding(inflater: LayoutInflater) =
        SimilarControllerBinding.inflate(inflater)
}

