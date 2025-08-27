package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.toLibraryManga
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryComposePresenter(
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
) : BaseCoroutinePresenter<LibraryComposeController>() {
    private val _libraryScreenState =
        MutableStateFlow(
            LibraryScreenState(
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
            )
        )

    val libraryScreenState: StateFlow<LibraryScreenState> = _libraryScreenState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        presenterScope.launchIO {
            val db = Injekt.get<DatabaseHelper>()
            val libraryMangaList =
                db.getFavoriteMangaList().executeOnIO().map { dbManga -> dbManga.toLibraryManga() }
            val allCategoryItem =
                CategoryItem(
                    id = -1,
                    name = "All",
                    order = -1,
                    mangaSort =
                        (LibrarySort.valueOf(libraryPreferences.sortingMode().get())
                                ?: LibrarySort.Title)
                            .categoryValue,
                    isDynamic = true,
                )
            val allLibraryCategoryItem =
                LibraryCategoryItem(
                    categoryItem = allCategoryItem,
                    libraryMangaList.toPersistentList(),
                )
            _libraryScreenState.update { it.copy(items = persistentListOf(allLibraryCategoryItem)) }
            /**
             * Group by categories, or dynamic categories libraryPreferences.sortingMode().get(),
             * libraryPreferences.sortAscending().get(),
             */
            // update

            // lookup unread count then update
            // lookup download count then update if enabled

        }
    }

    private var searchJob: Job? = null

    fun toggleIncognitoMode() {
        presenterScope.launchIO { securityPreferences.incognitoMode().toggle() }
    }

    fun refreshing(start: Boolean) {
        presenterScope.launchIO { _libraryScreenState.update { it.copy(isRefreshing = start) } }
    }

    fun search(searchQuery: String?) {
        searchJob?.cancel()
        searchJob = presenterScope.launchIO {}
    }
}
