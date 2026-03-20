import os
import re

filepath = "app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt"
with open(filepath, "r") as f: content = f.read()

content = content.replace("""            _mangaDetailScreenState.update { state ->
                state.copy(
                    allChapters =
                        updateChapterListForDownloadState(
                            chapterId = download.chapterItem.id,
                            downloadStatus = download.status,
                            downloadProgress = download.progress,
                            chapters = state.chapters.allChapters,
                        ),
                    activeChapters =
                        updateChapterListForDownloadState(
                            chapterId = download.chapterItem.id,
                            downloadStatus = download.status,
                            downloadProgress = download.progress,
                            chapters = state.chapters.activeChapters,
                        ),
                    searchChapters =
                        updateChapterListForDownloadState(
                            chapterId = download.chapterItem.id,
                            downloadStatus = download.status,
                            downloadProgress = download.progress,
                            chapters = state.general.searchChapters,
                        ),
                )
            }""", """            _mangaDetailScreenState.update { state ->
                state.copy(
                    chapters = state.chapters.copy(
                        allChapters = updateChapterListForDownloadState(download.chapterItem.id, download.status, download.progress, state.chapters.allChapters),
                        activeChapters = updateChapterListForDownloadState(download.chapterItem.id, download.status, download.progress, state.chapters.activeChapters)
                    ),
                    general = state.general.copy(
                        searchChapters = updateChapterListForDownloadState(download.chapterItem.id, download.status, download.progress, state.general.searchChapters)
                    )
                )
            }""")

content = content.replace("""                _mangaDetailScreenState.update { state ->
                    state.copy(
                        allChapters =
                            updateChapterListForDownloadState(
                                chapterId = chapterItem.chapter.id,
                                downloadStatus = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                                chapters = state.chapters.allChapters,
                            ),
                        activeChapters =
                            updateChapterListForDownloadState(
                                chapterId = chapterItem.chapter.id,
                                downloadStatus = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                                chapters = state.chapters.activeChapters,
                            ),
                        searchChapters =
                            updateChapterListForDownloadState(
                                chapterId = chapterItem.chapter.id,
                                downloadStatus = Download.State.NOT_DOWNLOADED,
                                downloadProgress = 0,
                                chapters = state.general.searchChapters,
                            ),
                    )
                }""", """                _mangaDetailScreenState.update { state ->
                    state.copy(
                        chapters = state.chapters.copy(
                            allChapters = updateChapterListForDownloadState(chapterItem.chapter.id, Download.State.NOT_DOWNLOADED, 0, state.chapters.allChapters),
                            activeChapters = updateChapterListForDownloadState(chapterItem.chapter.id, Download.State.NOT_DOWNLOADED, 0, state.chapters.activeChapters)
                        ),
                        general = state.general.copy(
                            searchChapters = updateChapterListForDownloadState(chapterItem.chapter.id, Download.State.NOT_DOWNLOADED, 0, state.general.searchChapters)
                        )
                    )
                }""")

with open(filepath, "w") as f: f.write(content)
