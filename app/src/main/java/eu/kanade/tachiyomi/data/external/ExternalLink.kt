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
    override val name = "Mangadex"
    override fun getLogo() = R.drawable.ic_tracker_mangadex_logo
    override fun getLogoColor() = Color.rgb(43, 48, 53)
    override fun getUrl() = "${MdUtil.baseUrl}/title/$id"
}

abstract class ExternalLink(val id: String) {
    abstract val name: String

    @DrawableRes
    abstract fun getLogo(): Int

    abstract fun getLogoColor(): Int

    abstract fun getUrl(): String
}
