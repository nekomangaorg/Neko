package eu.kanade.tachiyomi.util.system

fun Long.toMangaCacheKey(): String {
    return "manga-id-$this"
}
