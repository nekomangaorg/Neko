package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import org.nekomanga.domain.track.TrackServiceItem

class TrackManager(context: Context) {

    companion object {
        const val MDLIST = 0
        const val MYANIMELIST = 1
        const val ANILIST = 2
        const val KITSU = 3
        const val MANGA_UPDATES = 7

        fun isValidTracker(id: Int): Boolean {
            return arrayOf(MDLIST, MYANIMELIST, ANILIST, KITSU, MANGA_UPDATES).contains(id)
        }
    }

    val mdList = MdList(context, MDLIST)

    val myAnimeList = MyAnimeList(context, MYANIMELIST)

    val aniList = Anilist(context, ANILIST)

    val kitsu = Kitsu(context, KITSU)

    val mangaUpdates = MangaUpdates(context, MANGA_UPDATES)

    val services = hashMapOf(mdList.id to mdList, myAnimeList.id to myAnimeList, aniList.id to aniList, kitsu.id to kitsu, mangaUpdates.id to mangaUpdates)

    fun getService(id: Int) = services[id]

    fun hasLoggedServices() = services.values.any { it.isLogged() }

    fun getIdFromManga(trackService: TrackServiceItem, manga: Manga): String? {
        return when (trackService.id) {
            MDLIST -> MdUtil.getMangaUUID(manga.url)
            MYANIMELIST -> manga.my_anime_list_id
            ANILIST -> manga.anilist_id
            KITSU -> manga.kitsu_id
            // MANGA_UPDATES -> manga.manga_updates_id
            else -> null
        }
    }
}
