package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.composethemeadapter3.Mdc3Theme
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.MangaListWithHeader
import eu.kanade.tachiyomi.ui.base.components.Action
import eu.kanade.tachiyomi.ui.base.components.CoverRippleTheme
import eu.kanade.tachiyomi.ui.base.components.EmptyView
import eu.kanade.tachiyomi.ui.base.components.MangaGridWithHeader
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.manga.similar.SimilarPresenter
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.util.view.numberOfColumnsForCompose
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class SimilarController(bundle: Bundle? = null) :
    BaseComposeController<SimilarPresenter>(bundle) {

    constructor(manga: Manga) : this(
        Bundle().apply {
            putString(BrowseSourceController.MANGA_ID, MdUtil.getMangaId(manga.url))
        }
    )

    override var presenter =
        SimilarPresenter(bundle!!.getString(BrowseSourceController.MANGA_ID) ?: "")

    private val preferences: PreferencesHelper by injectLazy()

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        viewScope.launch {
            presenter.getSimilarManga()
        }


        binding.holder.setContent {
            /*
            doesnt work as the appbar has the padding added to it, and so does not ever draw under.  Need to use accompianst appbar, but it doesnt have scroll effects
            which make the collapse toolbar easy.
            val systemUiController = rememberSystemUiController()
                val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() < .5
                SideEffect {
                    XLog.e("setting system bars color")
                    systemUiController.setStatusBarColor(Color.Transparent, darkIcons = useDarkIcons)
                }*/

            Mdc3Theme {
                ProvideWindowInsets {

                    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }
                    val isRefreshing by presenter.isRefreshing.observeAsState(initial = true)
                    val isList by preferences.browseAsList().asFlow()
                        .collectAsState(preferences.browseAsList().get())

                    val refreshing: () -> Unit = {
                        viewScope.launch {
                            presenter.getSimilarManga(true)
                        }
                    }

                    val mangaClicked: (Manga) -> Unit = { manga ->
                        router.pushController(MangaDetailsController(manga,
                            true).withFadeTransaction())
                    }
                    Scaffold(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar =
                        {
                            CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
                                CenterAlignedTopAppBar(
                                    colors = TopAppBarDefaults.smallTopAppBarColors(
                                        scrolledContainerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.statusBarsPadding(),
                                    title = {
                                        Text(text = stringResource(id = R.string.similar),
                                            style = TextStyle(
                                                fontFamily = Typefaces.montserrat,
                                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                                fontWeight = FontWeight.Normal))
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { activity?.onBackPressed() }) {
                                            Icon(imageVector = Icons.Filled.ArrowBack,
                                                contentDescription = stringResource(id = R.string.back))
                                        }

                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            preferences.browseAsList().set(isList.not())
                                        }) {
                                            Icon(
                                                imageVector = if (isList.not()) {
                                                    Icons.Filled.ViewList
                                                } else {
                                                    Icons.Filled.ViewModule
                                                },
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                contentDescription = stringResource(id = R.string.display_as)
                                            )
                                        }
                                    },
                                    scrollBehavior = scrollBehavior
                                )
                            }
                        })
                    {
                        SwipeRefresh(
                            state = rememberSwipeRefreshState(isRefreshing),
                            onRefresh = refreshing,
                            modifier = Modifier.fillMaxSize(),
                            indicator = { state, trigger ->
                                SwipeRefreshIndicator(
                                    state = state,
                                    modifier = Modifier
                                        .zIndex(1f),
                                    refreshTriggerDistance = trigger,
                                    backgroundColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            },
                            content = {
                                SimilarContent(isRefreshing,
                                    isList,
                                    preferences.libraryLayout().get() == 2,
                                    refreshing,
                                    mangaClicked,
                                    view)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SimilarContent(
        isRefreshing: Boolean,
        isList: Boolean,
        isComfortable: Boolean,
        refreshing: () -> Unit,
        mangaClicked: (Manga) -> Unit,
        view: View,
    ) {
        val groupedManga: Map<Int, List<DisplayManga>> by presenter.mangaMap.observeAsState(
            emptyMap())
        if (isRefreshing.not()) {
            if (groupedManga.isEmpty()) {
                EmptyView(
                    iconicImage = CommunityMaterial.Icon.cmd_compass_off,
                    iconSize = 176.dp,
                    message = R.string.no_results_found,
                    actions = listOf(Action(R.string.retry, refreshing))
                )
            } else {
                val contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.navigationBars,
                    applyBottom = true,
                    applyTop = false)

                val groupedMangaRedux =
                    groupedManga.entries.map { stringResource(id = it.key) to it.value }.toMap()

                if (isList) {
                    MangaListWithHeader(groupedManga = groupedMangaRedux,
                        shouldOutlineCover = preferences.outlineOnCovers()
                            .get(),
                        contentPadding = contentPadding,
                        onClick = mangaClicked)
                } else {

                    val columns =
                        view.measuredWidth.numberOfColumnsForCompose(
                            preferences.gridSize()
                                .get())

                    MangaGridWithHeader(
                        groupedManga = groupedMangaRedux,
                        shouldOutlineCover = preferences.outlineOnCovers()
                            .get(),
                        columns = columns,
                        isComfortable = isComfortable,
                        contentPadding = contentPadding,
                        onClick = mangaClicked
                    )
                }
            }
        }
    }
}

