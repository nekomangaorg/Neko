Wait! In `updateDynamicCover`, the aggregate volumes are parsed from `db.getMangaAggregate(mangaId).executeOnIO()`. If it's null, it calls `mangaUseCases.updateMangaAggregate(...)`.
We need to trigger `updateMangaAggregate` if the flow returns null. But we shouldn't trigger it on every collect of the hot flow.
Actually, `MangaViewModel.kt` `onRefresh` will probably call `mangaUpdateCoordinator.update(...)` which updates aggregate data. Let's just use what's in the DB for the flow, and if `updateDynamicCover` triggers `updateMangaAggregate`, the flow will update automatically!
