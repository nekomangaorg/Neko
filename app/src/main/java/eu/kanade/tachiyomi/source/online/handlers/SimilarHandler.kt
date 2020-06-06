package eu.kanade.tachiyomi.source.online.handlers

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.json.JSONArray
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SimilarHandler(val preferences: PreferencesHelper) {

    /**
     * fetch our similar mangas
     */
    fun fetchSimilar(manga: Manga): Observable<MangasPage> {

        // Parse the Mangadex id from the URL
        val mangaid = MdUtil.getMangaId(manga.url).toLong()

        val lowQualityCovers = preferences.lowQualityCovers()

        // Get our current database
        val db = Injekt.get<DatabaseHelper>()
        val similarMangaDb = db.getSimilar(mangaid).executeAsBlocking() ?: return Observable.just(MangasPage(mutableListOf(), false))

        // Check if we have a result

        // Loop through and create a manga for each match
        // Note: we say this is not initialized so the browser presenter can load it
        // Note: the browser presenter will load the one from db or pull the latest details
        val similarMangaTitles = JSONArray(similarMangaDb.matched_titles)
        val similarMangaIds = JSONArray(similarMangaDb.matched_ids)
        val similarMangas = mutableListOf<SManga>()
        for (i in 0 until similarMangaIds.length()) {
            val matchedManga = SManga.create()
            val id = similarMangaIds.getLong(i)
            matchedManga.title = similarMangaTitles.getString(i)
            matchedManga.url = "/manga/$id/"
            matchedManga.thumbnail_url = MdUtil.formThumbUrl(matchedManga.url, lowQualityCovers)
            similarMangas.add(matchedManga)
        }

        // Return the matches
        return Observable.just(MangasPage(similarMangas, false))
    }
}
