package eu.kanade.tachiyomi.data.external

import android.graphics.Color
import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil

class AnimePlanet(id: String) : ExternalLink(id) {
    override val name = "AnimePlanet"
    override fun getLogo() = R.drawable.ic_tracker_anime_planet_logo
    override fun getLogoColor() = Color.rgb(24, 47, 98)
    override fun getUrl() = "https://www.anime-planet.com/manga/$id"
}

class BookWalker(id: String) : ExternalLink(id) {
    override val name = "Book Walker"
    override fun getLogo() = R.drawable.ic_tracker_bookwalker_logo
    override fun getLogoColor() = Color.rgb(255, 255, 255)
    override fun getUrl() = "https://bookwalker.jp/$id"
}

class EBookJapan(id: String) : ExternalLink(id) {
    override val name = "ebook Japan"
    override fun getLogo() = R.drawable.ic_tracker_ebj_logo
    override fun getLogoColor() = Color.rgb(255, 255, 255)
    override fun getUrl() = id
}

class CdJapan(id: String) : ExternalLink(id) {
    override val name = "CDJapan"
    override fun getLogo() = R.drawable.ic_tracker_cdj_logo
    override fun getLogoColor() = Color.rgb(0, 153, 204)
    override fun getUrl() = id
}

class Amazon(id: String) : ExternalLink(id) {
    override val name = "Amazon"
    override fun getLogo() = R.drawable.ic_tracker_amazon_logo
    override fun getLogoColor() = Color.rgb(25, 47, 62)
    override fun getUrl() = id
}

class MangaUpdatesLink(id: String) : ExternalLink(id) {
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

class DexApi(id: String) : ExternalLink(id) {
    override val name = "API"
    override fun getLogo() = R.drawable.ic_tracker_mangadex_logo
    override fun getLogoColor() = Color.rgb(43, 48, 53)
    override fun getUrl() =
        "${MdUtil.apiUrl}/manga/$id/feed?limit=500&contentRating[]=${MdConstants.ContentRating.safe}&contentRating[]=${MdConstants.ContentRating.suggestive}&contentRating[]=${MdConstants.ContentRating.erotica}&contentRating[]=${MdConstants.ContentRating.pornographic}&includes[]=${MdConstants.Types.scanlator}&order[volume]=desc&order[chapter]=desc"
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

    fun isOther() = getLogo() == R.drawable.ic_other_text_logo

    abstract fun getLogoColor(): Int

    abstract fun getUrl(): String
}
