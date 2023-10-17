package eu.kanade.tachiyomi.ui.source.latest

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.core.os.BundleCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.DisplayScreen

class DisplayController(private val displayScreenType: DisplayScreenType) :
    BaseComposeController<DisplayPresenter>(
        Bundle().apply {
            putParcelable(Display_Type, displayScreenType)
        },
    ) {

    constructor(bundle: Bundle) : this(bundle.getParcelable<DisplayScreenType>(Display_Type)!!)

    override var presenter = DisplayPresenter(displayScreenType)

    @Composable
    override fun ScreenContent() {
        DisplayScreen(
            displayScreenState = presenter.displayScreenState.collectAsStateWithLifecycle(),
            switchDisplayClick = presenter::switchDisplayMode,
            switchLibraryVisibilityClick = presenter::switchLibraryVisibility,
            onBackPress = router::handleBack,
            openManga = { mangaId: Long -> router.pushController(MangaDetailController(mangaId).withFadeTransaction()) },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::loadNextItems,
        )
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateMangaForChanges()
    }

    companion object {
        const val Display_Type = "displayType"
    }
}
