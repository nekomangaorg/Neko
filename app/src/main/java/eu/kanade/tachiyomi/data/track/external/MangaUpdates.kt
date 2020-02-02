package eu.kanade.tachiyomi.data.track.mdlist

import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService

class MangaUpdates(id: Int) : TrackService(id) {


    override val name = "MangaUpdates"

    override fun getLogo(): Int {
        return R.drawable.ic_tracker_manga_updates_logo
    }

    override fun getLogoColor(): Int {
        return Color.rgb(137, 164, 195)
    }

    override fun isExternalLink() = true

    companion object {
        val URL = "https://www.mangaupdates.com/series.html?id="
    }
}
