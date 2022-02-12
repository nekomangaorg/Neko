package eu.kanade.tachiyomi.ui.source

import android.os.Bundle
import android.view.View
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding
import com.google.android.material.composethemeadapter3.Mdc3Theme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.PagingListManga
import eu.kanade.tachiyomi.ui.base.components.CoverRippleTheme
import eu.kanade.tachiyomi.ui.base.components.PagingMangaGrid
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.view.numberOfColumnsForCompose
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import uy.kohesive.injekt.injectLazy

class LatestController(bundle: Bundle? = null) :
    BaseComposeController<LatestPresenter>(bundle) {

    override var presenter = LatestPresenter()

    private val preferences: PreferencesHelper by injectLazy()

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.holder.setContent {

            Mdc3Theme {
                ProvideWindowInsets {

                    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

                    val isList by preferences.browseAsList().asFlow()
                        .collectAsState(preferences.browseAsList().get())

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
                                        Text(text = stringResource(id = R.string.latest),
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
                        val contentPadding = rememberInsetsPaddingValues(
                            insets = LocalWindowInsets.current.navigationBars,
                            applyBottom = true,
                            applyTop = false)

                        val mangaList = presenter.mangaList.collectAsLazyPagingItems()

                        if (isList) {
                            PagingListManga(mangaList = mangaList,
                                shouldOutlineCover = preferences.outlineOnCovers()
                                    .get(),
                                contentPadding = contentPadding,
                                onClick = mangaClicked)
                        } else {

                            val columns =
                                view.rootView.measuredWidth.numberOfColumnsForCompose(
                                    preferences.gridSize()
                                        .get())

                            PagingMangaGrid(
                                mangaList = mangaList,
                                shouldOutlineCover = preferences.outlineOnCovers()
                                    .get(),
                                columns = columns,
                                isComfortable = preferences.libraryLayout().get() == 2,
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
