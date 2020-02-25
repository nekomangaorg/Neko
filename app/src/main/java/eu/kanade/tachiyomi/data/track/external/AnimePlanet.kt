package eu.kanade.tachiyomi.data.track.mdlist

import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService

class AnimePlanet(id: Int) : TrackService(id) {

    override val name = "AnimePlanet"

    override fun getLogo(): Int {
        return R.drawable.ic_tracker_anime_planet_logo
    }

    override fun getLogoColor(): Int {
        return Color.rgb(24, 47, 98)
    }

    override fun isExternalLink() = true

    companion object {
        val URL = "https://www.anime-planet.com/manga/"
    }
}
