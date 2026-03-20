import os
import re

filepath = "app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt"
with open(filepath, "r") as f: content = f.read()

content = content.replace("mangaDetailScreenState.value.snackbarColor", "mangaDetailScreenState.value.general.snackbarColor")
content = content.replace("mangaDetailScreenState.update { it.copy(isSearching = searchActive) }", "mangaDetailScreenState.update { it.copy(general = it.general.copy(isSearching = searchActive)) }")
content = content.replace("mangaDetailScreenState.update {\n                it.copy(searchChapters = filteredChapters.toPersistentList())\n            }", "mangaDetailScreenState.update {\n                it.copy(general = it.general.copy(searchChapters = filteredChapters.toPersistentList()))\n            }")
content = content.replace("_mangaDetailScreenState.value.allCategories", "_mangaDetailScreenState.value.category.allCategories")
content = content.replace("mangaDetailScreenState.update { it.copy(trackSearchResult = result) }", "mangaDetailScreenState.update { it.copy(track = it.track.copy(trackSearchResult = result)) }")
content = content.replace("mangaDetailScreenState.update {\n                it.copy(mergeSearchResult = MergeSearchResult.Loading)\n            }", "mangaDetailScreenState.update {\n                it.copy(merge = it.merge.copy(mergeSearchResult = MergeSearchResult.Loading))\n            }")

with open(filepath, "w") as f: f.write(content)
