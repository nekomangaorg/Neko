package eu.kanade.tachiyomi.source.online.handlers

import android.content.ContentValues.TAG
import android.content.res.Resources
import android.util.Log
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.RelatedMatch
import eu.kanade.tachiyomi.source.online.handlers.serializers.RelatedPageResult
import eu.kanade.tachiyomi.source.online.handlers.serializers.RelatedResult
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.serialization.json.Json
import rx.Observable
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import java.io.File

class RelatedHandler {



    private val preferences by injectLazy<PreferencesHelper>()


    /**
     * fetch our related mangas
     */
    fun fetchReleated(manga: SManga): Observable<MangasPage> {


        // load these results from disk
        //var result = File("/storage/emulated/0/Download/mangas_compressed.json").readText(Charsets.UTF_8)
        var result = File(preferences.relatedFilePath().get()).readText(Charsets.UTF_8)
        //var result = this::class.java.classLoader?.getResource("/res/raw/mangas_compressed.json")?.readText()
        //var result = MainActivity::class.java.classLoader?.getResource("/SDCARD/TEMP/mangas_compressed.json")?.readText()
        //val result = Resources.getSystem().openRawResource(R.raw.mangas_compressed).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val relatedPageResult = Json.nonstrict.parse(RelatedPageResult.serializer(), result)

        // Check if we have a result
        if (relatedPageResult.result.isEmpty()) {
            return Observable.just(MangasPage(mutableListOf(), false))
        }

        // Loop through and find the matching url with this manga id
        var found: Boolean = false
        var relatedMatchesResult: RelatedResult = RelatedResult(-1,"Unknown", emptyList())
        for (item in relatedPageResult.result) {
            if("/manga/${item.id}/" == manga.url) {
                found = true
                relatedMatchesResult = item
            }
        }

        // Check if we have a result
        if (!found || relatedMatchesResult.matches.isEmpty()) {
            return Observable.just(MangasPage(mutableListOf(), false))
        }

        // Else lets try to parse the matches
        val follows = relatedMatchesResult.matches.map {
            relatedFromElement(it)
        }
        return Observable.just(MangasPage(follows, false))

    }

    /**
     * Parse result element to manga
     */
    private fun relatedFromElement(result: RelatedMatch): SManga {
        val manga = SManga.create()
        manga.title = MdUtil.cleanString(result.title)
        manga.thumbnail_url = "${MdUtil.cdnUrl}/images/manga/${result.id}.jpg"
        manga.url = "/manga/${result.id}/"
        return manga
    }



}