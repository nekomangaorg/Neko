import re

with open("app/src/main/java/org/nekomanga/presentation/screens/stats/StatsViewModel.kt", "r") as f:
    content = f.read()

# Fix the mapNotNull / track mapping logic
search_block = """                val detailedStatMangaList =
                    libraryList
                        .mapNotNull { manga ->
                            val id = manga.id ?: return@mapNotNull null
                            val history = historyMap[id] ?: emptyList()
                            val tracks = tracksMap[id] ?: emptyList()
                            val mangaCategoryNames =
                                categoryMap[id]
                                    ?.mapNotNull { mc -> allCategories[mc.category_id]?.name }
                                    ?.takeUnless { names -> names.isEmpty() }
                                    ?: listOf(defaultCategoryName)

                            DetailedStatManga(
                                id = id,
                                title = manga.title,
                                type = MangaType.fromLangFlag(manga.lang_flag),
                                status = MangaStatus.fromStatus(manga.status),
                                contentRating =
                                    MangaContentRating.getContentRating(manga.getContentRating()),
                                totalChapters = manga.totalChapters,
                                readChapters = manga.read,
                                bookmarkedChapters = manga.bookmarkCount,
                                unavailableChapters = manga.unavailableCount,
                                readDuration = getReadDurationFromHistory(history),
                                startYear = getStartYear(history),
                                rating = manga.rating?.toDoubleOrNull()?.roundToTwoDecimal(),
                                tags = (manga.getGenres() ?: emptyList()).toPersistentList(),
                                userScore = getUserScore(tracks),
                                trackers =
                                    tracks
                                        .mapNotNull { trackManager.getService(it.sync_id) }
                                        .map { prefs.context.getString(it.nameRes()) }
                                        .toPersistentList(),
                                categories = mangaCategoryNames.sorted().toPersistentList(),
                            )
                        }"""

replace_block = """                val detailedStatMangaList =
                    libraryList
                        .mapNotNull { manga ->
                            val mangaId = manga.id ?: return@mapNotNull null
                            val history = historyMap[mangaId] ?: emptyList()
                            val tracks = tracksMap[mangaId] ?: emptyList()
                            val mangaCategoryNames =
                                categoryMap[mangaId]
                                    ?.mapNotNull { mc -> allCategories[mc.category_id]?.name }
                                    ?.takeUnless { names -> names.isEmpty() }
                                    ?: listOf(defaultCategoryName)

                            DetailedStatManga(
                                id = mangaId,
                                title = manga.title,
                                type = MangaType.fromLangFlag(manga.lang_flag),
                                status = MangaStatus.fromStatus(manga.status),
                                contentRating =
                                    MangaContentRating.getContentRating(manga.getContentRating()),
                                totalChapters = manga.totalChapters,
                                readChapters = manga.read,
                                bookmarkedChapters = manga.bookmarkCount,
                                unavailableChapters = manga.unavailableCount,
                                readDuration = getReadDurationFromHistory(history),
                                startYear = getStartYear(history),
                                rating = manga.rating?.toDoubleOrNull()?.roundToTwoDecimal(),
                                tags = (manga.getGenres() ?: emptyList()).toPersistentList(),
                                userScore = getUserScore(tracks),
                                trackers =
                                    tracks
                                        .mapNotNull { track -> trackManager.getService(track.sync_id) }
                                        .map { service -> prefs.context.getString(service.nameRes()) }
                                        .toPersistentList(),
                                categories = mangaCategoryNames.sorted().toPersistentList(),
                            )
                        }"""
content = content.replace(search_block, replace_block)

with open("app/src/main/java/org/nekomanga/presentation/screens/stats/StatsViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/eu/kanade/tachiyomi/util/chapter/SourceChapterSorter.kt", "r") as f:
    content = f.read()

search_block = """            while (iteratorToCurrentValues.isNotEmpty()) {
                val smallestEntry = iteratorToCurrentValues.minWithOrNull(c)!!

                yield(smallestEntry.value)

                if (!smallestEntry.key.hasNext()) iteratorToCurrentValues.remove(smallestEntry.key)
                else iteratorToCurrentValues[smallestEntry.key] = smallestEntry.key.next()
            }"""
replace_block = """        while (iteratorToCurrentValues.isNotEmpty()) {
            val smallestEntry = iteratorToCurrentValues.minWithOrNull(c) ?: break

            yield(smallestEntry.value)

            if (!smallestEntry.key.hasNext()) iteratorToCurrentValues.remove(smallestEntry.key)
            else iteratorToCurrentValues[smallestEntry.key] = smallestEntry.key.next()
        }"""
content = content.replace(search_block, replace_block)
with open("app/src/main/java/eu/kanade/tachiyomi/util/chapter/SourceChapterSorter.kt", "w") as f:
    f.write(content)


print("done")
