package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.manga.similar.SimilarPresenter
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.util.view.numberOfColumnsForCompose
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.launch
import org.nekomanga.presentation.components.ListGridActionButton
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.screens.Action
import org.nekomanga.presentation.screens.EmptyScreen
import uy.kohesive.injekt.injectLazy

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class SimilarController(bundle: Bundle? = null) :
    BaseComposeController<SimilarPresenter>(bundle) {

    constructor(manga: Manga) : this(
        Bundle().apply {
            putString(BrowseSourceController.MANGA_ID, MdUtil.getMangaId(manga.url))
        },
    )

    override var presenter =
        SimilarPresenter(bundle!!.getString(BrowseSourceController.MANGA_ID) ?: "")

    private val preferences: PreferencesHelper by injectLazy()

    @Composable
    override fun ScreenContent() {
        val scope = rememberCoroutineScope()
        LaunchedEffect(key1 = Unit) {
            scope.launch {
                presenter.getSimilarManga()
            }
        }

        val refreshing: () -> Unit = {
            viewScope.launch {
                presenter.getSimilarManga(true)
            }
        }

        val isRefreshing by presenter.isRefreshing.observeAsState(initial = true)
        val isList by preferences.browseAsList().asFlow()
            .collectAsState(preferences.browseAsList().get())

        val mangaClicked: (Manga) -> Unit = { manga ->
            router.pushController(
                MangaDetailsController(
                    manga,
                    true,
                ).withFadeTransaction(),
            )
        }

        NekoScaffold(
            title = stringResource(id = R.string.similar),
            onNavigationIconClicked = { activity?.onBackPressed() },
            actions = {
                ListGridActionButton(
                    isList = isList,
                    buttonClicked = { preferences.browseAsList().set(isList.not()) },
                )
            },
        ) { paddingValues ->
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = refreshing,
                modifier = Modifier
                    .fillMaxSize(),
                clipIndicatorToPadding = false,
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshingOffset = paddingValues.calculateTopPadding(),
                        refreshTriggerDistance = trigger,
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,

                        )
                },
                content = {
                    SimilarContent(
                        isRefreshing,
                        isList,
                        preferences.libraryLayout().get() == 2,
                        paddingValues = paddingValues,
                        refreshing,
                        mangaClicked,
                    )
                },
            )
        }
    }

    @Composable
    private fun SimilarContent(
        isRefreshing: Boolean,
        isList: Boolean,
        isComfortable: Boolean,
        paddingValues: PaddingValues = PaddingValues(),
        refreshing: () -> Unit,
        mangaClicked: (Manga) -> Unit,
    ) {
        val groupedManga: Map<Int, List<DisplayManga>> by presenter.mangaMap.observeAsState(
            emptyMap(),
        )
        if (isRefreshing.not()) {
            if (groupedManga.isEmpty()) {
                EmptyScreen(
                    iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                    iconSize = 176.dp,
                    message = R.string.no_results_found,
                    actions = listOf(Action(R.string.retry, refreshing)),
                )
            } else {
                val contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        .asPaddingValues().calculateBottomPadding(),
                    top = paddingValues.calculateTopPadding(),
                )

                val groupedMangaRedux =
                    groupedManga.entries.associate { stringResource(id = it.key) to it.value }

                if (isList) {
                    MangaListWithHeader(
                        groupedManga = groupedMangaRedux,
                        shouldOutlineCover = preferences.outlineOnCovers()
                            .get(),
                        contentPadding = contentPadding,
                        onClick = mangaClicked,
                    )
                } else {
                    val columns =
                        binding.root.rootView.measuredWidth.numberOfColumnsForCompose(
                            preferences.gridSize()
                                .get(),
                        )

                    MangaGridWithHeader(
                        groupedManga = groupedMangaRedux,
                        shouldOutlineCover = preferences.outlineOnCovers()
                            .get(),
                        columns = columns,
                        isComfortable = isComfortable,
                        contentPadding = contentPadding,
                        onClick = mangaClicked,
                    )
                }
            }
        }
    }
}
