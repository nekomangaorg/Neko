sed -i '/fun getMangasByUrl/,$d' app/src/main/java/eu/kanade/tachiyomi/data/database/queries/MangaQueries.kt
sed -i 's/^}//g' app/src/main/java/eu/kanade/tachiyomi/data/database/queries/MangaQueries.kt
cat << 'INNEREOF' >> app/src/main/java/eu/kanade/tachiyomi/data/database/queries/MangaQueries.kt
    fun getMangasByUrl(urls: List<String>, sourceId: Long) = db.get()
        .listOfObjects(Manga::class.java)
        .withQuery(RawQuery.builder()
            .query("SELECT * FROM ${MangaTable.TABLE} WHERE ${MangaTable.COL_URL} IN (${Queries.placeholders(urls.size)}) AND ${MangaTable.COL_SOURCE} = ?")
            .args(*(urls + sourceId.toString()).toTypedArray())
            .observesTables(MangaTable.TABLE)
            .build())
        .withGetResolver(MangaGetResolver())
        .prepare()
}
INNEREOF
