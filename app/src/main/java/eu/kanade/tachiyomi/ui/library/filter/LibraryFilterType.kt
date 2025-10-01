package eu.kanade.tachiyomi.ui.library.filter

import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.UiText

interface LibraryFilterType {
    fun toInt(): Int

    fun UiText(): UiText

    fun matches(item: LibraryMangaItem): Boolean

    fun toggle(enabling: Boolean): LibraryFilterType
}
