import os
import re

filepath = "app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt"
with open(filepath, "r") as f: content = f.read()

manga_fields = ["initialized", "inLibrary", "isMerged", "currentTitle", "originalTitle", "alternativeTitles", "artist", "author", "currentDescription", "genres", "status", "isPornographic", "langFlag", "missingChapters", "estimatedMissingChapters", "stats", "lastVolume", "lastChapter", "externalLinks", "currentArtwork", "alternativeArtwork", "dynamicCovers"]
general_fields = ["isRefreshing", "isSearching", "firstLoad", "snackbarColor", "incognitoMode", "forcePortrait", "themeBasedOffCovers", "vibrantColor", "hasDefaultCategory", "hideButtonText", "backdropSize", "wrapAltTitles", "searchChapters", "removedChapters"]
chapter_fields = ["activeChapters", "allChapters", "nextUnreadChapter", "chapterFilter", "chapterFilterText", "chapterSortFilter", "chapterScanlatorFilter", "chapterSourceFilter", "chapterLanguageFilter", "allScanlators", "allUploaders", "allSources", "allLanguages"]
track_fields = ["tracks", "loggedInTrackService", "trackServiceCount", "trackingSuggestedDates", "trackSearchResult"]
category_fields = ["allCategories", "currentCategories"]
merge_fields = ["validMergeTypes", "mergeSearchResult"]

# we only want to replace fields inside `_mangaDetailScreenState.update { state -> state.copy(...) }` block
# but an easier regex is replacing `state.copy(\s*(.*))` with nested copies. Wait, that's hard.

# Let's do manual replacement. We will just use `sed` or simple replace on the known updates.
