package eu.kanade.tachiyomi.data.external

import android.graphics.Color
import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.utils.MdUtil

class AnimePlanet(id: String) : ExternalLink(id) {
    override val name = "AnimePlanet"
    override fun getLogo() = R.drawable.ic_tracker_anime_planet_logo
    override fun getLogoColor() = Color.rgb(24, 47, 98)
    override fun getUrl() = "https://www.anime-planet.com/manga/$id"
}

class MangaUpdates(id: String) : ExternalLink(id) {
    override val name = "MangaUpdates"
    override fun getLogo() = R.drawable.ic_tracker_manga_updates_logo
    override fun getLogoColor() = Color.rgb(137, 164, 195)
    override fun getUrl() = "https://www.mangaupdates.com/series.html?id=$id"
}

class Dex(id: String) : ExternalLink(id) {
    override val name = "MangaDex"
    override fun getLogo() = R.drawable.ic_tracker_mangadex_logo
    override fun getLogoColor() = Color.rgb(43, 48, 53)
    override fun getUrl() = "${MdUtil.baseUrl}/title/$id"
}

class Raw(id: String) : ExternalLink(id) {
    override val name = "Raw"
    override fun getLogo() = R.drawable.ic_other_text_logo
    override fun getLogoColor() = Color.rgb(255, 209, 220)
    override fun getUrl() = id
}

class Engtl(id: String) : ExternalLink(id) {
    override val name = "English TL"
    override fun getLogo() = R.drawable.ic_other_text_logo
    override fun getLogoColor() = Color.rgb(255, 209, 220)
    override fun getUrl() = id
}

class AniList(id: String) : ExternalLink(id) {
    override val name = "AniList"
    override fun getLogo() = R.drawable.ic_tracker_anilist_logo
    override fun getLogoColor() = Color.rgb(18, 25, 35)
    override fun getUrl() = "https://anilist.co/manga/$id"
}

class Mal(id: String) : ExternalLink(id) {
    override val name = "MyAnimeList"
    override fun getLogo() = R.drawable.ic_tracker_mal_logo
    override fun getLogoColor() = Color.rgb(46, 81, 162)
    override fun getUrl() = "https://myanimelist.net/manga/$id"
}

class Kitsu(id: String) : ExternalLink(id) {
    override val name = "Kitsu"
    override fun getLogo() = R.drawable.ic_tracker_kitsu_logo
    override fun getLogoColor() = Color.rgb(51, 37, 50)
    override fun getUrl() = "https://kitsu.io/manga/$id"
}

sealed class ExternalLink(val id: String) {
    abstract val name: String

    @DrawableRes
    abstract fun getLogo(): Int

    abstract fun getLogoColor(): Int

    abstract fun getUrl(): String
}
