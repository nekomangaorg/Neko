package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class LibraryManga : MangaImpl() {

    var unread: Int = 0

    var category: Int = 0

    fun mangaType(): Int {
        val sourceManager:SourceManager by injectLazy()
        return if (currentGenres()?.split(",")?.any
            { tag ->
                val trimmedTag = tag.trim().toLowerCase(Locale.getDefault())
                trimmedTag == "long strip" || trimmedTag == "manwha"
            } == true ||
            sourceManager.getOrStub(source).name.contains("webtoon", true))
            MANWHA
        else MANGA
    }

    companion object {
        const val MANGA = 1
        const val MANWHA = 2
    }

}