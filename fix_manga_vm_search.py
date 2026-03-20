import os
import re

filepath = "app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt"
with open(filepath, "r") as f: content = f.read()

content = content.replace("""                    _mangaDetailScreenState.update {
                        when (mergedMangaResults.isEmpty()) {
                            true -> it.copy(mergeSearchResult = MergeSearchResult.NoResult)
                            false ->
                                it.copy(
                                    mergeSearchResult =
                                        MergeSearchResult.Success(mergedMangaResults)
                                )
                        }
                    }""", """                    _mangaDetailScreenState.update {
                        when (mergedMangaResults.isEmpty()) {
                            true -> it.copy(merge = it.merge.copy(mergeSearchResult = MergeSearchResult.NoResult))
                            false ->
                                it.copy(
                                    merge = it.merge.copy(mergeSearchResult =
                                        MergeSearchResult.Success(mergedMangaResults))
                                )
                        }
                    }""")

content = content.replace("""                    _mangaDetailScreenState.update {
                        it.copy(
                            mergeSearchResult =
                                MergeSearchResult.Error(
                                    error.message ?: "Error looking up information"
                                )
                        )
                    }""", """                    _mangaDetailScreenState.update {
                        it.copy(
                            merge = it.merge.copy(mergeSearchResult =
                                MergeSearchResult.Error(
                                    error.message ?: "Error looking up information"
                                ))
                        )
                    }""")

content = content.replace("""            _mangaDetailScreenState.update { state ->
                state.copy(chapterFilter = calculateNewFilter(state.chapterFilter, filterOption))
            }""", """            _mangaDetailScreenState.update { state ->
                state.copy(chapters = state.chapters.copy(chapterFilter = calculateNewFilter(state.chapters.chapterFilter, filterOption)))
            }""")

content = content.replace("""            _mangaDetailScreenState.update {
                it.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId))
            }""", """            _mangaDetailScreenState.update {
                it.copy(general = it.general.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId)))
            }""")

content = content.replace("""                when (searchActive) {
                    true -> {
                        mangaDetailScreenState.value.activeChapters.filter {""", """                when (searchActive) {
                    true -> {
                        mangaDetailScreenState.value.chapters.activeChapters.filter {""")

content = content.replace("mangaDetailScreenState.value.allChapters.size", "mangaDetailScreenState.value.chapters.allChapters.size")
content = content.replace("mangaDetailScreenState.value.activeChapters.size", "mangaDetailScreenState.value.chapters.activeChapters.size")
content = content.replace("mangaDetailScreenState.value.searchChapters", "mangaDetailScreenState.value.general.searchChapters")
content = content.replace("mangaDetailScreenState.value.activeChapters", "mangaDetailScreenState.value.chapters.activeChapters")
content = content.replace("mangaDetailScreenState.value.allChapters", "mangaDetailScreenState.value.chapters.allChapters")
content = content.replace("mangaDetailScreenState.value.isSearching", "mangaDetailScreenState.value.general.isSearching")

with open(filepath, "w") as f: f.write(content)
