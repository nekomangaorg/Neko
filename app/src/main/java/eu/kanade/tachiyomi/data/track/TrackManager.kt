package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.mdlist.AnimePlanet
import eu.kanade.tachiyomi.data.track.mdlist.MangaUpdates
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.Myanimelist

class TrackManager(context: Context) {

    companion object {
        const val MDLIST = 0
        const val MYANIMELIST = 1
        const val ANILIST = 2
        const val KITSU = 3
        const val ANIMEPLANET = 5
        const val MANGAUPDATES = 6
    }

    val mdList = MdList(context, MDLIST)

    val myAnimeList = Myanimelist(context, MYANIMELIST)

    val aniList = Anilist(context, ANILIST)

    val kitsu = Kitsu(context, KITSU)

    val animePlanet = AnimePlanet(ANIMEPLANET)

    val mangaUpdates = MangaUpdates(MANGAUPDATES)


    val services = listOf(mdList, aniList, kitsu, myAnimeList, animePlanet, mangaUpdates)

    fun getService(id: Int) = services.find { it.id == id }

    fun hasLoggedServices() = services.any { it.isLogged }

}
