Wait, if I change `MangaScreenChapterState` to include `activeChapters: PersistentMap<String, PersistentList<ChapterItem>>`, it breaks all usages.
Wait, let's look at `eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt`.
I can map `activeChapters` inside `MangaViewModel.kt` using `db.getMangaAggregate(mangaId).executeOnIO()`.
Since we can't change `activeChapters` to a map without rewriting a lot of `MangaViewModel.kt` actions (like `markPreviousChapters`, etc. that use indices), let's keep `activeChapters` as a list, and add `activeChaptersGrouped: PersistentMap<String, PersistentList<ChapterItem>>` to `MangaScreenChapterState`. Same for `searchChaptersGrouped` in `MangaScreenGeneralState`. Or simply pass a map to `chapterList` in `MangaScreen.kt`.

Wait, the prompt says "Update the MangaScreen to take a group of chapters instead of a list. The group being passed in are chapters mapped to the volume number. Use the aggregate data to figure out what volume. If there is no matching volume then put them in a no volume group."
This can mean we do the grouping *inside* `MangaScreen.kt` or `MangaViewModel.kt`. Doing it in `MangaViewModel.kt` is cleaner.

Let's read `MangaViewModel.kt` again.
