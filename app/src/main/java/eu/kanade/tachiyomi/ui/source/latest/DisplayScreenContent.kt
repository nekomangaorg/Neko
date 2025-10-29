package eu.kanade.tachiyomi.ui.source.latest

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.domain.manga.DisplayManga

sealed class DisplayScreenContent {
    data class List(val manga: PersistentList<DisplayManga>) : DisplayScreenContent()
    data class Graded(val manga: ImmutableMap<Int, PersistentList<DisplayManga>>) : DisplayScreenContent()
}
