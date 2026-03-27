Let's modify `MangaViewModel.kt`.

1. Add `aggregateFlow`:
```kotlin
    val aggregateFlow =
        db.getMangaAggregate(mangaId)
            .asRxObservable()
            .asFlow()
            .map { dbAggregate ->
                if (dbAggregate != null) {
                    Json.parseToJsonElement(dbAggregate.volumes).asMdMap<AggregateVolume>()
                } else null
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
```

2. Add it to `viewModelScope.launchIO` around line 269:
```kotlin
        viewModelScope.launchIO {
            combine(
                    combine(mangaFlow, staticChapterDataFlow, categoriesDataFlow, ::Triple),
                    combine(artworkFlow, tracksFlow, mergedFlow, ::Triple),
                    combine(
                        _mangaFilterState,
                        mangaDetailsPreferences.dynamicCovers().changes(),
                        historyFlow,
                        ::Triple,
                    ),
                    aggregateFlow,
                ) {
                    (mangaItem, staticChapterData, categoriesData),
                    (artworkList, tracks, IsMerged),
                    (filterState, dynamicCover, history),
                    aggregateVolumes ->
```

3. Inside the combine, after `val languageFilter = ...`, calculate `chapterVolumes`:
```kotlin
                            val chapterVolumes = staticChapterData.allChapters.associate { chapterItem ->
                                val chapter = chapterItem.chapter
                                val mangaDexChapterId = chapter.mangaDexChapterId
                                val chapterNumber = chapter.chapterNumber
                                val chapterNumberStr =
                                    if (chapterNumber % 1 == 0f) {
                                        chapterNumber.toInt().toString()
                                    } else {
                                        chapterNumber.toString()
                                    }
                                var volumeFromAggregate: String? = null
                                if (aggregateVolumes != null) {
                                    for ((_, volumeInfo) in aggregateVolumes) {
                                        val chaptersInVolume = volumeInfo.chapters.values
                                        val matchById =
                                            mangaDexChapterId != null &&
                                                chaptersInVolume.any {
                                                    it.id == mangaDexChapterId ||
                                                        it.others.contains(mangaDexChapterId)
                                                }
                                        val matchByNumber = chaptersInVolume.any { it.chapter == chapterNumberStr }

                                        if (matchById || matchByNumber) {
                                            volumeFromAggregate = volumeInfo.volume
                                            break
                                        }
                                    }
                                }
                                chapter.id to (volumeFromAggregate ?: "No Volume")
                            }
```

Wait! It's better to refactor `getVolumeForChapter` out since it is also used in `updateDynamicCover`? No, `updateDynamicCover` calculates it manually. Let's refactor it to a private helper function.
