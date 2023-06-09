package eu.kanade.tachiyomi.data.track.mangaupdates

import androidx.core.text.isDigitsOnly
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.manga.MangaMappings
import logcat.LogPriority
import org.nekomanga.core.loggycat
import uy.kohesive.injekt.injectLazy

object MangaUpdatesHelper {
    private val mappings: MangaMappings by injectLazy()

    fun getMangaUpdatesApiId(manga: Manga): String? {
        manga.manga_updates_id?.let { id ->
            // 200591 is the last ID of the old IDs
            return when (id.isDigitsOnly() && id.toLong() <= 200591) {
                true -> {
                    val newMuId = mappings.getMuNewForMuID(id)
                    if (newMuId == null) {
                        loggycat(LogPriority.ERROR) { "Missing MU ID mapping for id: $id" }
                    }
                    newMuId
                }

                false -> id.toLong(36).toString()
            }
            return if (id.isDigitsOnly() && id.toLong() <= 200591) {
                mappings.getMuNewForMuID(id)
            } else {
                id.toLong(36).toString()
            }
        }
        return null
    }
}
