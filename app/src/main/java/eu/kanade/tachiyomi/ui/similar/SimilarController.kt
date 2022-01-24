package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter3.Mdc3Theme
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.models.DisplayManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.SimilarControllerBinding
import eu.kanade.tachiyomi.ui.base.MangaListWithHeader
import eu.kanade.tachiyomi.ui.base.components.Action
import eu.kanade.tachiyomi.ui.base.components.CoverRippleTheme
import eu.kanade.tachiyomi.ui.base.components.EmptyView
import eu.kanade.tachiyomi.ui.base.components.MangaGridWithHeader
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
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
    BaseCoroutineController<SimilarControllerBinding, SimilarPresenter>(bundle) {

    constructor(manga: Manga) : this(
        Bundle().apply {
            putLong(BrowseSourceController.MANGA_ID, manga.id!!)
        }
    )

    override var presenter = SimilarPresenter(bundle!!.getLong(BrowseSourceController.MANGA_ID))

    private val preferences: PreferencesHelper by injectLazy()

    override fun getTitle(): String? {
        return view?.context?.getString(R.string.similar)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        hideToolbar()

        viewScope.launch {
            presenter.getSimilarManga()
        }


        binding.holder.setContent {

            val systemUiController = rememberSystemUiController()
            val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() < .5
            SideEffect {
                XLog.e("setting system bars color")
                systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = useDarkIcons)
            }

            Mdc3Theme {
                ProvideWindowInsets {

                    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }
                    val isRefreshing by presenter.isRefreshing.observeAsState(initial = true)

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
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .statusBarsPadding(),
                        topBar =
                        {
                            SmallTopAppBar(
                                colors = TopAppBarDefaults.smallTopAppBarColors(
                                    scrolledContainerColor = MaterialTheme.colorScheme.surface),
                                title = {
                                    Text(text = stringResource(id = R.string.similar),
                                        style = TextStyle(
                                            fontFamily = Typefaces.montserrat,
                                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                            fontWeight = FontWeight.Normal))
                                },
                                navigationIcon = {
                                    CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
                                        IconButton(onClick = { /*TODO*/ }) {
                                            Icon(imageVector = Icons.Filled.ArrowBack,
                                                contentDescription = "back button")
                                        }
                                    }
                                },
                                scrollBehavior = scrollBehavior
                            )
                        })
                    {
                        SwipeRefresh(
                            state = rememberSwipeRefreshState(isRefreshing),
                            onRefresh = refreshing,
                            indicator = { state, trigger ->
                                SwipeRefreshIndicator(
                                    state = state,
                                    refreshTriggerDistance = trigger,
                                    backgroundColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        ) {

                            val groupedManga: Map<String, List<DisplayManga>> by presenter.mangaMap.observeAsState(
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

                                    if (preferences.browseAsList().get()) {
                                        MangaListWithHeader(groupedManga = groupedManga,
                                            shouldOutlineCover = preferences.outlineOnCovers()
                                                .get(),
                                            contentPadding = contentPadding,
                                            onClick = mangaClicked)
                                    } else {

                                        val columns =
                                            view.measuredWidth.numberOfColumnsForCompose(
                                                preferences.gridSize()
                                                    .get())

                                        val comfortable = preferences.libraryLayout().get() == 2

                                        MangaGridWithHeader(
                                            groupedManga = groupedManga,
                                            shouldOutlineCover = preferences.outlineOnCovers()
                                                .get(),
                                            columns = columns,
                                            isComfortable = comfortable,
                                            contentPadding = contentPadding,
                                            onClick = mangaClicked
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView(view: View) {
        showToolbar()
        super.onDestroyView(view)
    }

    override fun createBinding(inflater: LayoutInflater) =
        SimilarControllerBinding.inflate(inflater)
}

