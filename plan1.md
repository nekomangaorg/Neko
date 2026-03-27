The crash occurs because `db.getMangaAggregate(mangaId).asRxObservable().asFlow()` expects the value emitted by `asRxObservable()` to be non-null. `asRxObservable()` under the hood uses `RxJava` logic that might emit a `null` value if the item is not found in the database. Wait, actually, `PreparedGetObject` in StorIO emits `null` if the object doesn't exist, and `asFlow()` is a Kotlin extension `fun <T> Observable<T>.asFlow(): Flow<T>` that expects a non-null `T`, unless it's explicitly typed as `Observable<T?>` or `asFlow()` allows nulls.

Let's check `eu.kanade.tachiyomi.util.system.RxJavaExtensionsKt$asFlow$1$observer$1.onNext`. The parameter `t` is defined as non-null `T`, but RxJava 1.x allows emitting `null`. When `null` is passed to `onNext(t: T)`, Kotlin throws a `NullPointerException` because `t` is marked non-null.

If `db.getMangaAggregate(mangaId)` returns an empty result (because there is no aggregate data yet), it emits `null`. To fix this, we can tell RxJava to handle nulls properly or convert them to an Optional, or use `.map { Optional.fromNullable(it) }` before calling `.asFlow()`.

Wait, StorIO `PreparedGetObject` provides a way to get an Optional, `db.getMangaAggregate(mangaId).asRxObservable()` might emit `null`.
Let's look at `MangaViewModel.kt` usages of `asRxObservable().asFlow()`.

```kotlin
    val mangaFlow =
        db.getManga(mangaId)
            .asRxObservable()
            .asFlow()
```
For `mangaFlow`, `Manga` is never null because the viewmodel is instantiated only if `Manga` exists.
For `historyFlow`: `db.getHistoryByMangaId(mangaId)` returns a List, so it emits an empty list instead of null.
For `aggregateFlow`, it's an object, so it emits `null` when it doesn't exist!

We need to make it emit `Optional<MangaAggregate>` or map nulls to something else.
Actually, StorIO provides `asRxObservable()` which we can chain with `.map { Optional.fromNullable(it) }` or just use `.map { it }` ? No, `RxJava` 1.x allows nulls. We can map the `null` inside RxJava to an Optional, and then use `asFlow()`.
```kotlin
import com.github.michaelbull.result.getOrElse
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
// Actually there is a com.fernandocejas.arrow.optional.Optional in older codebase? Or java.util.Optional?
```
Wait, we can just use `asRxObservable<MangaAggregate?>().asFlow()`? Let's check how `asFlow` is defined.
