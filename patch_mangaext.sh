cat << 'INNEREOF' >> app/src/main/java/eu/kanade/tachiyomi/util/manga/MangaExtensions.kt

fun Iterable<SourceManga>.toDisplayManga(db: DatabaseHelper, sourceId: Long): List<DisplayManga> {
    if (this.none()) return emptyList()

    val urls = this.map { it.url }.distinct()
    val localMangas = mutableMapOf<String, Manga>()

    // Process in chunks of 500 to avoid SQLite limits
    urls.chunked(500).forEach { chunk ->
        val result = db.getMangasByUrl(chunk, sourceId).executeAsBlocking()
        result.forEach { localMangas[it.url] = it }
    }

    val newMangasToInsert = mutableListOf<Manga>()
    val existingMangasToUpdate = mutableListOf<Manga>()

    val resultMangas = this.map { sourceManga ->
        var localManga = localMangas[sourceManga.url]
        if (localManga == null) {
            val newManga = Manga.create(sourceManga.url, sourceManga.title, sourceId)
            newManga.apply { this.thumbnail_url = currentThumbnail }
            newMangasToInsert.add(newManga)
            localManga = newManga
        } else if (localManga.title.isBlank()) {
            localManga.title = sourceManga.title
            existingMangasToUpdate.add(localManga)
        }
        localManga to sourceManga
    }

    if (newMangasToInsert.isNotEmpty()) {
        val insertResults = db.insertMangas(newMangasToInsert).executeAsBlocking()
        insertResults.results().entries.forEachIndexed { index, entry ->
            newMangasToInsert[index].id = entry.value.insertedId()
        }
    }
    if (existingMangasToUpdate.isNotEmpty()) {
        db.insertMangas(existingMangasToUpdate).executeAsBlocking()
    }

    return resultMangas.map { (localManga, sourceManga) ->
        localManga.toDisplayManga(sourceManga.displayText, sourceManga.displayTextRes)
    }
}
INNEREOF
