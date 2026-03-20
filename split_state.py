import os
import re

files = [
    "app/src/main/java/org/nekomanga/presentation/screens/MangaScreen.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/MangaScreenTopBar.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/MangaDetailsHeader.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/DetailsBottomSheet.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/TrackingSheet.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/ArtworkSheet.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/CategoriesSheet.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/MergeSheet.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/ExternalLinksSheet.kt",
    "app/src/main/java/org/nekomanga/presentation/screens/manga/FilterChapterSheet.kt"
]

manga_fields = ["initialized", "inLibrary", "isMerged", "currentTitle", "originalTitle", "alternativeTitles", "artist", "author", "currentDescription", "genres", "status", "isPornographic", "langFlag", "missingChapters", "estimatedMissingChapters", "stats", "lastVolume", "lastChapter", "externalLinks", "currentArtwork", "alternativeArtwork", "dynamicCovers"]
general_fields = ["isRefreshing", "isSearching", "firstLoad", "snackbarColor", "incognitoMode", "forcePortrait", "themeBasedOffCovers", "vibrantColor", "hasDefaultCategory", "hideButtonText", "backdropSize", "wrapAltTitles", "searchChapters", "removedChapters"]
chapter_fields = ["activeChapters", "allChapters", "nextUnreadChapter", "chapterFilter", "chapterFilterText", "chapterSortFilter", "chapterScanlatorFilter", "chapterSourceFilter", "chapterLanguageFilter", "allScanlators", "allUploaders", "allSources", "allLanguages"]
track_fields = ["tracks", "loggedInTrackService", "trackServiceCount", "trackingSuggestedDates", "trackSearchResult"]
category_fields = ["allCategories", "currentCategories"]
merge_fields = ["validMergeTypes", "mergeSearchResult"]

def replace_in_file(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, "r") as f: content = f.read()

    for f in manga_fields: content = re.sub(r'(\w+)\.' + f + r'\b', r'\1.manga.' + f, content)
    for f in general_fields: content = re.sub(r'(\w+)\.' + f + r'\b', r'\1.general.' + f, content)
    for f in chapter_fields: content = re.sub(r'(\w+)\.' + f + r'\b', r'\1.chapters.' + f, content)
    for f in track_fields: content = re.sub(r'(\w+)\.' + f + r'\b', r'\1.track.' + f, content)
    for f in category_fields: content = re.sub(r'(\w+)\.' + f + r'\b', r'\1.category.' + f, content)
    for f in merge_fields: content = re.sub(r'(\w+)\.' + f + r'\b', r'\1.merge.' + f, content)

    with open(filepath, "w") as f: f.write(content)

for file in files:
    replace_in_file(file)

print("Done replacing.")
