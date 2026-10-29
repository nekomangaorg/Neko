package org.nekomanga.domain.browse

import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import org.nekomanga.domain.manga.DisplayManga

/** Domain model representing a section of manga displayed on the Browse screen. */
data class BrowseHomeSection(
    val displayScreenType: DisplayScreenType,
    val mangaList: List<DisplayManga>,
)
