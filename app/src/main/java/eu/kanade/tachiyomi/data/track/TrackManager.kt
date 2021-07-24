package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList

class TrackManager(context: Context) {

    companion object {
        const val MDLIST = 0
        const val MYANIMELIST = 1
        const val ANILIST = 2
        const val KITSU = 3
    }

    val mdList = MdList(context, MDLIST)

    val myAnimeList = MyAnimeList(context, MYANIMELIST)

    val aniList = Anilist(context, ANILIST)

    val kitsu = Kitsu(context, KITSU)

    val services = listOf(mdList, myAnimeList, aniList, kitsu)

    fun getService(id: Int) = services.find { it.id == id }

    fun hasLoggedServices() = services.any { it.isLogged() }
}
