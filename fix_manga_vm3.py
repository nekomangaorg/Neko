import os
import re

filepath = "app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt"
with open(filepath, "r") as f: content = f.read()

content = content.replace("mangaDetailScreenState.update { it.copy(isRefreshing = true) }", "mangaDetailScreenState.update { it.copy(general = it.general.copy(isRefreshing = true)) }")
content = content.replace("mangaDetailScreenState.update { it.copy(isRefreshing = false) }", "mangaDetailScreenState.update { it.copy(general = it.general.copy(isRefreshing = false)) }")
content = content.replace("mangaDetailScreenState.update { it.copy(removedChapters = persistentListOf()) }", "mangaDetailScreenState.update { it.copy(general = it.general.copy(removedChapters = persistentListOf())) }")
content = content.replace("mangaDetailScreenState.update { it.copy(snackbarColor = snackbarColor) }", "mangaDetailScreenState.update { it.copy(general = it.general.copy(snackbarColor = snackbarColor)) }")

with open(filepath, "w") as f: f.write(content)
