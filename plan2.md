`fun <T : Any> Observable<T>.asFlow(): Flow<T>` explicitly requires `T` to be `Any` (non-null). Since `db.getMangaAggregate(mangaId).asRxObservable()` emits `null` when no aggregate data exists, passing `null` to `onNext` in `asFlow` crashes Kotlin.

To fix this, we should NOT use `asRxObservable().asFlow()` if it might emit `null`. Instead, we can observe `db.getMangaAggregate(mangaId)` using a different pattern, or wrap the null emission in `RxJava` BEFORE calling `asFlow()`.

For example, we can map the `MangaAggregate?` to a `Optional` or just a wrapper class in RxJava, then unwrap it in Kotlin Flow.
Or better, we can write a simple RxJava mapping:
```kotlin
data class AggregateResult(val aggregate: MangaAggregate?)

    val aggregateFlow =
        db.getMangaAggregate(mangaId)
            .asRxObservable()
            .map { AggregateResult(it) } // map null to AggregateResult(null)
            .asFlow()
            .map { result ->
                val dbAggregate = result.aggregate
                if (dbAggregate != null) {
                    Json.parseToJsonElement(dbAggregate.volumes).asMdMap<AggregateVolume>()
                } else null
            }
```
Wait, if `it` is null, `map { AggregateResult(it) }` will receive a null object in Kotlin. But if the Kotlin lambda is defined as `map { it -> AggregateResult(it) }`, `it` would be `MangaAggregate!` (platform type) or `MangaAggregate?`.

Let's test this wrapper.
