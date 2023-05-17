package eu.kanade.tachiyomi.source.online.utils

import eu.kanade.tachiyomi.ui.manga.MangaConstants

enum class MdSort(val displayName: String, val key: String, val state: MangaConstants.SortState) {
    Best("Best Match", MdConstants.Sort.relevance, MangaConstants.SortState.Descending),
    LatestUploads("Latest uploads", MdConstants.Sort.latest, MangaConstants.SortState.Descending),
    OldestUploads("Oldest uploads", MdConstants.Sort.latest, MangaConstants.SortState.Ascending),
    TitleDescending("Title asc", MdConstants.Sort.title, MangaConstants.SortState.Ascending),
    TitleAscending("Title desc", MdConstants.Sort.title, MangaConstants.SortState.Descending),
    HighestRating("Highest rating", MdConstants.Sort.rating, MangaConstants.SortState.Descending),
    LowestRating("lowest rating", MdConstants.Sort.rating, MangaConstants.SortState.Ascending),
    MostFollows("Most bookmarks", MdConstants.Sort.bookmarkCount, MangaConstants.SortState.Descending),
    LeastFollows("Fewest bookmarks", MdConstants.Sort.bookmarkCount, MangaConstants.SortState.Ascending),
    RecentlyAdded("Recently added", MdConstants.Sort.createdAt, MangaConstants.SortState.Descending),
    OldestAdded("Oldest added", MdConstants.Sort.createdAt, MangaConstants.SortState.Descending),
    YearAscending("Year asc", MdConstants.Sort.year, MangaConstants.SortState.Ascending),
    YearDescending("Year desc", MdConstants.Sort.year, MangaConstants.SortState.Descending),
    // updatedAt("Information updated", MdConstants.Sort.updatedAt),
}
