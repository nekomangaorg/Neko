import os

filepath = "app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt"
with open(filepath, "r") as f: content = f.read()

content = content.replace("_mangaDetailScreenState.value.inLibrary", "_mangaDetailScreenState.value.manga.inLibrary")
content = content.replace("mangaDetailScreenState.value.currentTitle", "mangaDetailScreenState.value.manga.currentTitle")
content = content.replace("mangaDetailScreenState.value.loggedInTrackService", "mangaDetailScreenState.value.track.loggedInTrackService")
content = content.replace("mangaDetailScreenState.value.tracks", "mangaDetailScreenState.value.track.tracks")
content = content.replace("mangaDetailScreenState.value.hasDefaultCategory", "mangaDetailScreenState.value.general.hasDefaultCategory")
content = content.replace("mangaDetailScreenState.value.allCategories", "mangaDetailScreenState.value.category.allCategories")

with open(filepath, "w") as f: f.write(content)
