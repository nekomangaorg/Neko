I need to group chapters into maps. The map should be of type `PersistentMap<String, PersistentList<ChapterItem>>`.
But there's an issue: If I change `searchChapters`, `activeChapters`, and `allChapters` to maps, it might break many existing functions in `MangaViewModel.kt` because they iterate over those lists or expect a list. So it's better to keep `activeChapters` and `searchChapters` as lists, but in `MangaScreen.kt`, the parameter should take a Map, or we can add `activeChapterGroups` and `searchChapterGroups` to the state. Or we can just modify `MangaScreen.kt` to group them on the fly? Wait, the instructions say:
"Update the MangaScreen to take a group of chapters instead of a list. The group being passed in are chapters mapped to the volume number. Use the aggregate data to figure out what volume. If there is no matching volume then put them in a no volume group."
"Then in the ui make a parent card with the volume text then list the chapters. For each entry"
This means the UI should take the map, so the transformation happens either in ViewModel or in UI. The prompt says "Update the MangaScreen to take a group of chapters instead of a list. ... Use the aggregate data to figure out what volume." This implies that the mapping is done inside `MangaScreen` or `MangaViewModel` and passed as a map.

Wait, if I change `chapterList` signature in `MangaScreen.kt`:
```kotlin
private fun LazyListScope.chapterList(
    chapters: PersistentMap<String, PersistentList<ChapterItem>>,
```
Then in `MangaViewModel.kt`, `AllChapterInfo` should have a map of `activeChapters`?
"Use the aggregate data to figure out what volume."

Let's do the grouping in `MangaViewModel.kt` and expose it as a map. Wait, if I change `activeChapters: PersistentList<ChapterItem>` to `PersistentMap<String, PersistentList<ChapterItem>>`, it breaks all usages.
Instead of replacing, let's create `val activeChapterMap: PersistentMap<String, PersistentList<ChapterItem>>` and `val searchChapterMap`.
But the user says "Update the MangaScreen to take a group of chapters instead of a list", specifically mentioning MangaScreen.
