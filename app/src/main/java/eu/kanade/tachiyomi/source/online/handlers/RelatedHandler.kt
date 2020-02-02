package eu.kanade.tachiyomi.source.online.handlers

import android.util.Log
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import rx.Observable
import org.json.JSONArray
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.regex.Pattern

class RelatedHandler {



    /**
     * fetch our related mangas
     */
    fun fetchReleated(manga: Manga): Observable<MangasPage> {

        // Parse the Mangadex id from the URL (should be first number)
        // Return if we can't find the id in the url
        val p = Pattern.compile("\\d+")
        val m = p.matcher(manga.url)
        if(!m.find() || m.group().isEmpty()) {
            return Observable.just(MangasPage(mutableListOf(), false))
        }
        var mangaid = m.group().toLong()

        // Get our current database
        var db = Injekt.get<DatabaseHelper>()
        var related_manga = db.getRelated(mangaid).executeAsBlocking()

        // Check if we have a result
        if (related_manga.size != 1) {
            return Observable.just(MangasPage(mutableListOf(), false))
        }

        // Loop through and create a manga for each match
        var arrTiles = JSONArray(related_manga[0].matched_titles)
        var arrMatched = JSONArray(related_manga[0].matched_ids)
        var arrMangas = arrayListOf<SManga>()
        for (i in 0 until arrMatched.length()) {

            // Get this related id
            val id = arrMatched.getLong(i)

            // Create the manga
            val manga_matched = SManga.create()
            manga_matched.title = arrTiles.getString(i)
            manga_matched.thumbnail_url = "${MdUtil.cdnUrl}/images/manga/${id}.jpg"
            manga_matched.url = "/manga/${id}/"
            arrMangas.add(manga_matched)

        }

        // Return the matches
        return Observable.just(MangasPage(arrMangas, false))

    }



}