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
import uy.kohesive.injekt.injectLazy

class RelatedHandler {

    private val preferences: PreferencesHelper by injectLazy()

    /**
     * fetch our related mangas
     */
    fun fetchRelated(manga: Manga): Observable<MangasPage> {

        // Parse the Mangadex id from the URL
        val mangaid = MdUtil.getMangaId(manga.url).toLong()

        // Get our current database
        val db = Injekt.get<DatabaseHelper>()
        val relatedMangasDb = db.getRelated(mangaid).executeAsBlocking()

        // Check if we have a result
        if (relatedMangasDb == null) {
            return Observable.just(MangasPage(mutableListOf(), false))
        }

        // Loop through and create a manga for each match
        val relatedMangaTitles = JSONArray(relatedMangasDb.matched_titles)
        val relatedMangaIds = JSONArray(relatedMangasDb.matched_ids)
        val relatedMangas = mutableListOf<SManga>()
        for (i in 0 until relatedMangaIds.length()) {
            val matchedManga = SManga.create()
            val id = relatedMangaIds.getLong(i)
            matchedManga.title = relatedMangaTitles.getString(i)
            matchedManga.url = "/manga/$id/"
            matchedManga.thumbnail_url = MdUtil.formThumbUrl(matchedManga.url, preferences.lowQualityCovers())
            relatedMangas.add(matchedManga)
        }

        // Return the matches
        return Observable.just(MangasPage(relatedMangas, false))
    }
}
