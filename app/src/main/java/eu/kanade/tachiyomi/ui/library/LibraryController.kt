package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.source.browse.BrowseController
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.toLibraryManga
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory

class LibraryController : BaseComposeController<LibraryPresenter>() {
    override val presenter = LibraryPresenter()

    @Composable override fun ScreenContent() {}

    private fun openManga(mangaId: Long) {
        viewScope.launchUI {
            router.pushController(MangaDetailController(mangaId).withFadeTransaction())
        }
    }

    fun search(searchQuery: String) {
        presenter.search(searchQuery)
    }

    private fun updateCategory(category: CategoryItem, context: Context) {
        LibraryUpdateJob.startNow(
            context = context,
            category.toDbCategory(),
            mangaToUse =
                if (category.isDynamic) {
                    val libraryItems =
                        presenter.libraryScreenState.value.items
                            .firstOrNull { it.categoryItem.id == category.id }
                            ?.libraryItems
                            ?.map { it.toLibraryManga() }
                    libraryItems
                } else {
                    null
                },
        )
    }

    private fun searchMangaDex(query: String) {
        router.setRoot(
            BrowseController(query).withFadeTransaction().tag(R.id.nav_browse.toString())
        )
    }

    private fun shareManga(context: Context) {
        val urls = presenter.getSelectedMangaUrls()
        if (urls.isEmpty()) return
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/*"
                putExtra(Intent.EXTRA_TEXT, urls)
            }
        startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }

    private fun updateLibrary(context: Context) {
        if (!LibraryUpdateJob.isRunning(context)) {
            LibraryUpdateJob.startNow(context)
        }
    }
}
