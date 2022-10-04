package eu.kanade.tachiyomi.ui.source.latest

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.DisplayScreen

class DisplayController(private val displayScreenType: DisplayScreenType, private val headerTitle: String = "") :
    BaseComposeController<DisplayPresenter>(
        Bundle().apply {
            putSerializable(Display_Type, displayScreenType)
            putString(AppBar_Title, headerTitle)
        },
    ) {

    constructor(bundle: Bundle) : this(bundle.getSerializable(Display_Type)!! as DisplayScreenType, bundle.getString(AppBar_Title) ?: "")

    override var presenter = DisplayPresenter(displayScreenType)

    @Composable
    override fun ScreenContent() {
        DisplayScreen(
            displayScreenState = presenter.displayScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            onBackPress = { activity?.onBackPressed() },
            openManga = { mangaId: Long -> router.pushController(MangaDetailController(mangaId).withFadeTransaction()) },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::loadNextItems,
        )
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateCovers()
    }

    companion object {
        const val Display_Type = "displayType"
        const val AppBar_Title = "appBarTitle"
    }
}
