Since `activeChapters` and `searchChapters` are heavily used as a flat list, changing their type to a Map will break a lot of functionality (downloading, removing, filtering, next unread chapter logic).
Therefore, the best approach is:
1. Keep `activeChapters` and `searchChapters` as `PersistentList<ChapterItem>`.
2. Add new properties `activeChaptersGrouped: PersistentMap<String, PersistentList<ChapterItem>>` and `searchChaptersGrouped: PersistentMap<String, PersistentList<ChapterItem>>` (or just group them on the fly in the UI or ViewModel).
But wait, if we group them inside the ViewModel, where do we get the grouping logic? We can fetch the aggregate data in `MangaViewModel.kt`'s main flow.

Let's look at `staticChapterDataFlow`. No, let's create a flow `aggregateDataFlow` in `MangaViewModel.kt` that emits `Map<String, AggregateVolume>?`.
```kotlin
    val aggregateDataFlow =
        db.getMangaAggregate(mangaId)
            .asFlow()
            .map { dbAggregate ->
                if (dbAggregate != null) {
                    Json.parseToJsonElement(dbAggregate.volumes).asMdMap<AggregateVolume>()
                } else null
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
```
Wait! `getMangaAggregate` isn't returning a Flow right now! Let's check `DatabaseHelper`.
