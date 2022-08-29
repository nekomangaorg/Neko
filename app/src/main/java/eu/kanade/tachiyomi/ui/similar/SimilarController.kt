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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.manga.similar.SimilarPresenter
import eu.kanade.tachiyomi.util.view.numberOfColumnsForCompose
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.components.ListGridActionButton
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.screens.Action
import org.nekomanga.presentation.screens.IconicsEmptyScreen
import uy.kohesive.injekt.injectLazy

/**
 * Controller that shows the similar/related manga
 */
class SimilarController(mangaUUID: String) : BaseComposeController<SimilarPresenter>() {

    constructor(bundle: Bundle) : this(bundle.getString(MANGA_EXTRA) ?: "")

    override var presenter = SimilarPresenter(mangaUUID)

    private val preferences: PreferencesHelper by injectLazy()

    @Composable
    override fun ScreenContent() {

        val isList by preferences.browseAsList().asFlow()
            .collectAsState(preferences.browseAsList().get())


        NekoScaffold(
            title = stringResource(id = R.string.similar),
            onNavigationIconClicked = { activity?.onBackPressed() },
            actions = {
                ListGridActionButton(
                    isList = isList,
                    buttonClicked = { preferences.browseAsList().set(isList.not()) },
                )
            },
        ) { incomingPaddingValues ->
            val isRefreshing = presenter.isRefreshing.collectAsState()
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing.value),
                onRefresh = presenter::refresh,
                modifier = Modifier.fillMaxSize(),
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        refreshingOffset = (incomingPaddingValues.calculateTopPadding() * 2),
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,

                        )
                },
            ) {
                SimilarContent(
                    isRefreshing = isRefreshing.value,
                    isList = isList,
                    isComfortable = preferences.libraryLayout().get() == 2,
                    paddingValues = incomingPaddingValues,
                    refreshing = presenter::refresh,
                    mangaClicked = { mangaId ->
                        router.pushController(
                            MangaDetailController(mangaId).withFadeTransaction(),
                        )
                    },
                )
            }

        }
    }

    @Composable
    private fun SimilarContent(
        isRefreshing: Boolean,
        isList: Boolean,
        isComfortable: Boolean,
        paddingValues: PaddingValues = PaddingValues(),
        refreshing: () -> Unit,
        mangaClicked: (Long) -> Unit,
    ) {
        val groupedManga: Map<Int, List<DisplayManga>> by presenter.mangaMap.collectAsState()
        if (isRefreshing.not()) {
            if (groupedManga.isEmpty()) {
                IconicsEmptyScreen(
                    iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                    iconSize = 176.dp,
                    message = stringResource(id = R.string.no_results_found),
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

    companion object {
        const val MANGA_EXTRA = "manga"
    }
}

