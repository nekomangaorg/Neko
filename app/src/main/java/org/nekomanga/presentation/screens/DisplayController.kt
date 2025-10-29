package org.nekomanga.presentation.screens

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.view.withFadeTransaction

class DisplayController(private val displayScreenType: DisplayScreenType) :
    BaseComposeController<DisplayPresenter>(
        Bundle().apply { putParcelable(DISPLAY_TYPE, displayScreenType) },
    ) {

    constructor(bundle: Bundle) : this(bundle.getParcelable<DisplayScreenType>(DISPLAY_TYPE)!!)

    override var presenter = DisplayPresenter(displayScreenType)

    @Composable
    override fun ScreenContent() {
        DisplayScreen(
            displayScreenState = presenter.displayScreenState.collectAsStateWithLifecycle(),
            displayScreenType = displayScreenType,
            switchDisplayClick = presenter::switchDisplayMode,
            libraryEntryVisibilityClick = presenter::switchLibraryEntryVisibility,
            onBackPress = router::handleBack,
            openManga = { mangaId: Long ->
                router.pushController(MangaDetailController(mangaId).withFadeTransaction())
            },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::loadNextItems,
            onRefresh = presenter::refresh,
        )
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateMangaForChanges()
    }

    companion object {
        const val DISPLAY_TYPE = "displayType"
    }
}
