package eu.kanade.tachiyomi.data.external

import androidx.annotation.DrawableRes
import androidx.core.text.isDigitsOnly
import org.nekomanga.R
import org.nekomanga.constants.MdConstants

data class AnimePlanet(override val id: String) : ExternalLink() {
    override val name = "AnimePlanet"
    override val logo = R.drawable.ic_tracker_anime_planet_logo
    override val logoColor: Long = 0xFF182F62

    override fun getUrl() = "https://www.anime-planet.com/manga/$id"
}

data class BookWalker(override val id: String) : ExternalLink() {
    override val name = "Book Walker"
    override val logo = R.drawable.ic_tracker_bookwalker_logo
    override val logoColor: Long = 0xFFFFFFFF
    override val onLogoColor: Long = 0xFF000000

    override fun getUrl() = "https://bookwalker.jp/$id"
}

data class EBookJapan(override val id: String) : ExternalLink() {
    override val name = "ebook Japan"
    override val logo = R.drawable.ic_tracker_ebj_logo
    override val logoColor: Long = 0xFFFFFFFF
    override val onLogoColor: Long = 0xFF000000

    override fun getUrl() = id
}

data class CdJapan(override val id: String) : ExternalLink() {
    override val name = "CDJapan"
    override val logo = R.drawable.ic_tracker_cdj_logo
    override val logoColor: Long = 0xFF0099CC

    override fun getUrl() = id
}

data class Amazon(override val id: String) : ExternalLink() {
    override val name = "Amazon"
    override val logo = R.drawable.ic_tracker_amazon_logo
    override val logoColor: Long = 0xFF192F3E

    override fun getUrl() = id
}

data class MangaUpdatesLink(override val id: String) : ExternalLink() {
    override val name = "MangaUpdates"
    override val logo = R.drawable.ic_tracker_manga_updates_logo
    override val logoColor: Long = 0xFF89a4c3

    // 200591 is the last ID of the old IDs
    override fun getUrl() =
        when (id.isDigitsOnly() && id.toLong() <= 200591) {
            true -> "https://www.mangaupdates.com/series.html?id=$id"
            false -> "https://www.mangaupdates.com/series/$id"
        }
}

data class Dex(override val id: String) : ExternalLink() {
    override val name = "MangaDex"
    override val logo = R.drawable.ic_tracker_mangadex_logo
    override val logoColor: Long = 0xFF2B3035

    override fun getUrl() = "${MdConstants.baseUrl}/title/$id"
}

data class DexComments(override val id: String) : ExternalLink() {
    override val name = "Comments"
    override val logo = R.drawable.ic_tracker_mangadex_logo
    override val logoColor: Long = 0xFF2B3035

    override fun getUrl() = "${MdConstants.forumUrl}${id}"
}

data class DexApi(override val id: SCtring) : ExternalLink() {
    override val name = "API"
    override val logo = R.drawable.ic_tracker_mangadex_logo
    override val logoColor: Long = 0xFF2B3035

    override fun getUrl() =
        "${MdConstants.Api.baseUrl}/manga/$id/feed?limit=500&contentRating[]=${MdConstants.ContentRating.safe}&contentRating[]=${MdConstants.ContentRating.suggestive}&contentRating[]=${MdConstants.ContentRating.erotica}&contentRating[]=${MdConstants.ContentRating.pornographic}&includes[]=${MdConstants.Types.scanlator}&order[volume]=desc&order[chapter]=desc"
}

data class Raw(override val id: String) : ExternalLink() {
    override val name = "Raw"
    override val showLogo: Boolean = false
    override val logoColor: Long = 0XFFA52A2A

    override fun getUrl() = id
}

data class Engtl(override val id: String) : ExternalLink() {
    override val name = "English TL"
    override val showLogo: Boolean = false
    override val logoColor: Long = 0xFF2F4F4F

    override fun getUrl() = id
}

data class AniList(override val id: String) : ExternalLink() {
    override val name = "AniList"
    override val logo = R.drawable.ic_tracker_anilist_logo
    override val logoColor: Long = 0xFF121923

    override fun getUrl() = "https://anilist.co/manga/$id"
}

data class Mal(override val id: String) : ExternalLink() {
    override val name = "MyAnimeList"
    override val logo = R.drawable.ic_tracker_mal_logo
    override val logoColor: Long = 0xFF2E51A2

    override fun getUrl() = "https://myanimelist.net/manga/$id"
}

data class Kitsu(override val id: String) : ExternalLink() {
    override val name = "Kitsu"
    override val logo = R.drawable.ic_tracker_kitsu_logo
    override val logoColor: Long = 0xFF332532

    override fun getUrl() = "https://kitsu.app/manga/$id"
}

sealed class ExternalLink {
    abstract val id: String
    abstract val name: String

    @DrawableRes open val logo: Int = 0
    open val showLogo: Boolean = true
    abstract val logoColor: Long
    open val onLogoColor: Long = 0xFFFFFFFF

    abstract fun getUrl(): String
}
