package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.filter.Filter

sealed class BrowseAction {
    object Retry : BrowseAction()

    object LoadNextPage : BrowseAction()

    object ToggleIncognito : BrowseAction()

    object RandomManga : BrowseAction()

    object SwitchDisplayMode : BrowseAction()

    object SwitchLibraryVisibility : BrowseAction()

    data class SetScreenType(
        val browseScreenType: BrowseScreenType,
        val forceUpdate: Boolean = false
    ) : BrowseAction()

    data class CreatorSearch(val creator: String) : BrowseAction()

    data class TagSearch(val tag: String) : BrowseAction()

    data class OtherClick(val uuid: String) : BrowseAction()

    data class AddNewCategory(val newCategory: String) : BrowseAction()

    data class ToggleFavorite(val mangaId: Long, val categoryItems: List<CategoryItem>) :
        BrowseAction()

    data class SaveFilter(val name: String) : BrowseAction()

    data class LoadFilter(val browseFilterImpl: BrowseFilterImpl) : BrowseAction()

    data class DeleteFilter(val name: String) : BrowseAction()

    object ResetFilter : BrowseAction()

    data class MarkFilterAsDefault(val name: String, val makeDefault: Boolean) : BrowseAction()

    data class FilterChanged(val filter: Filter) : BrowseAction()
}