package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.json.JSONArray
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RelatedHandler {

    /**
     * fetch our related mangas
     */
    fun fetchRelated(manga: Manga): Observable<MangasPage> {

        // Parse the Mangadex id from the URL
        var mangaid = MdUtil.getMangaId(manga.url).toLong()

        // Get our current database
        var db = Injekt.get<DatabaseHelper>()
        var relatedMangasDb = db.getRelated(mangaid).executeAsBlocking()

        // Check if we have a result
        if (relatedMangasDb == null) {
            return Observable.just(MangasPage(mutableListOf(), false))
        }

        // Loop through and create a manga for each match
        var relatedMangaTitles = JSONArray(relatedMangasDb.matched_titles)
        var relatedMangaIds = JSONArray(relatedMangasDb.matched_ids)
        var relatedMangas = mutableListOf<SManga>()
        for (i in 0 until relatedMangaIds.length()) {
            val matchedManga = SManga.create()
            val id = relatedMangaIds.getLong(i)
            matchedManga.title = relatedMangaTitles.getString(i)
            matchedManga.thumbnail_url = "${MdUtil.cdnUrl}/images/manga/$id.jpg"
            matchedManga.url = "/manga/$id/"
            relatedMangas.add(matchedManga)
        }

        // Return the matches
        return Observable.just(MangasPage(relatedMangas, false))
    }
}
