package eu.kanade.tachiyomi.data.external

import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil

class AnimePlanet(id: String) : ExternalLink(id) {
    override val name = "AnimePlanet"
    override val logo = R.drawable.ic_tracker_anime_planet_logo
    override val logoColor: Long = 0xFF182F62

    override fun getUrl() = "https://www.anime-planet.com/manga/$id"
}

class BookWalker(id: String) : ExternalLink(id) {
    override val name = "Book Walker"
    override val logo = R.drawable.ic_tracker_bookwalker_logo
    override val logoColor: Long = 0xFFFFFFFF
    override val onLogoColor: Long = 0xFF000000
    override fun getUrl() = "https://bookwalker.jp/$id"
}

class EBookJapan(id: String) : ExternalLink(id) {
    override val name = "ebook Japan"
    override val logo = R.drawable.ic_tracker_ebj_logo
    override val logoColor: Long = 0xFFFFFFFF
    override val onLogoColor: Long = 0xFF000000
    override fun getUrl() = id
}

class CdJapan(id: String) : ExternalLink(id) {
    override val name = "CDJapan"
    override val logo = R.drawable.ic_tracker_cdj_logo
    override val logoColor: Long = 0xFF0099CC
    override fun getUrl() = id
}

class Amazon(id: String) : ExternalLink(id) {
    override val name = "Amazon"
    override val logo = R.drawable.ic_tracker_amazon_logo
    override val logoColor: Long = 0xFF192F3E
    override fun getUrl() = id
}

class MangaUpdatesLink(id: String) : ExternalLink(id) {
    override val name = "MangaUpdates"
    override val logo = R.drawable.ic_tracker_manga_updates_logo
    override val logoColor: Long = 0xFF89a4c3
    override fun getUrl() = "https://www.mangaupdates.com/series.html?id=$id"
}

class Dex(id: String) : ExternalLink(id) {
    override val name = "MangaDex"
    override val logo = R.drawable.ic_tracker_mangadex_logo
    override val logoColor: Long = 0xFF2B3035
    override fun getUrl() = "${MdUtil.baseUrl}/title/$id"
}

class DexApi(id: String) : ExternalLink(id) {
    override val name = "API"
    override val logo = R.drawable.ic_tracker_mangadex_logo
    override val logoColor: Long = 0xFF2B3035
    override fun getUrl() =
        "${MdUtil.apiUrl}/manga/$id/feed?limit=500&contentRating[]=${MdConstants.ContentRating.safe}&contentRating[]=${MdConstants.ContentRating.suggestive}&contentRating[]=${MdConstants.ContentRating.erotica}&contentRating[]=${MdConstants.ContentRating.pornographic}&includes[]=${MdConstants.Types.scanlator}&order[volume]=desc&order[chapter]=desc"
}

class Raw(id: String) : ExternalLink(id) {
    override val name = "Raw"
    override val showLogo: Boolean = false
    override val logoColor: Long = 0XFFA52A2A
    override fun getUrl() = id
}

class Engtl(id: String) : ExternalLink(id) {
    override val name = "English TL"
    override val showLogo: Boolean = false
    override val logoColor: Long = 0xFF2F4F4F
    override fun getUrl() = id
}

class AniList(id: String) : ExternalLink(id) {
    override val name = "AniList"
    override val logo = R.drawable.ic_tracker_anilist_logo
    override val logoColor: Long = 0xFF121923
    override fun getUrl() = "https://anilist.co/manga/$id"
}

class Mal(id: String) : ExternalLink(id) {
    override val name = "MyAnimeList"
    override val logo = R.drawable.ic_tracker_mal_logo
    override val logoColor: Long = 0xFF2E51A2
    override fun getUrl() = "https://myanimelist.net/manga/$id"
}

class Kitsu(id: String) : ExternalLink(id) {
    override val name = "Kitsu"
    override val logo = R.drawable.ic_tracker_kitsu_logo
    override val logoColor: Long = 0xFF332532
    override fun getUrl() = "https://kitsu.io/manga/$id"
}

sealed class ExternalLink(val id: String) {
    abstract val name: String

    @DrawableRes
    open val logo: Int = 0

    open val showLogo: Boolean = true

    abstract val logoColor: Long

    open val onLogoColor: Long = 0xFFFFFFFF

    abstract fun getUrl(): String
}
