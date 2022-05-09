package eu.kanade.tachiyomi.ui.source.latest

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.PagingListManga
import eu.kanade.tachiyomi.ui.base.components.ListGridActionButton
import eu.kanade.tachiyomi.ui.base.components.NekoScaffold
import eu.kanade.tachiyomi.ui.base.components.PagingMangaGrid
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.view.numberOfColumnsForCompose
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import uy.kohesive.injekt.injectLazy

class LatestController(bundle: Bundle? = null) :
    BaseComposeController<LatestPresenter>(bundle) {

    override var presenter = LatestPresenter()

    private val preferences: PreferencesHelper by injectLazy()

    @Composable
    override fun ScreenContent() {

        val isList by preferences.browseAsList().asFlow()
            .collectAsState(preferences.browseAsList().get())

        val mangaClicked: (Manga) -> Unit = { manga ->
            router.pushController(MangaDetailsController(manga,
                true).withFadeTransaction())
        }

        NekoScaffold(
            title = R.string.latest, onBack = { activity?.onBackPressed() },
            actions = {
                ListGridActionButton(isList = isList,
                    buttonClicked = { preferences.browseAsList().set(isList.not()) })
            },
        ) { incomingContentPadding ->
            val contentPadding =
                PaddingValues(bottom = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    .asPaddingValues().calculateBottomPadding(),
                    top = incomingContentPadding.calculateTopPadding())

            val mangaListPagingItems = presenter.mangaList.collectAsLazyPagingItems()

            if (isList) {
                PagingListManga(mangaListPagingItems = mangaListPagingItems,
                    shouldOutlineCover = preferences.outlineOnCovers()
                        .get(),
                    contentPadding = contentPadding,
                    onClick = mangaClicked)
            } else {

                val columns =
                    binding.root.measuredWidth.numberOfColumnsForCompose(
                        preferences.gridSize()
                            .get())

                PagingMangaGrid(
                    mangaListPagingItems = mangaListPagingItems,
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
