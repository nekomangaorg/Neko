package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.data.database.models.MangaImpl
import java.io.Serializable

interface SManga : Serializable {

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: String?

    var status: Int

    var thumbnail_url: String?

    var initialized: Boolean

    fun currentTitle(): String {
        val splitTitle = title.split(splitter)
        return splitTitle.first()
    }

    fun originalTitle(): String {
        val splitTitle = title.split(splitter)
        return splitTitle.last()
    }

    fun currentGenres() = split(genre, true)

    fun originalGenres() = split(genre, false)

    fun currentDesc() = split(description, true)

    fun originalDesc() = split(description, false)

    fun currentAuthor() = split(author, true)

    fun originalAuthor() = split(author, false)

    fun currentArtist() = split(artist, true)

    fun originalArtist() = split(artist, false)

    private fun split(string: String?, first: Boolean):String? {
        val split = string?.split(splitter) ?: return null
        val s = if (first) split.first() else split.last()
        return if (s.isBlank()) null else s
    }

    fun copyFrom(other: SManga) {
        if (other.author != null)
            author = if (currentAuthor() != originalAuthor()) {
                val current = currentAuthor()
                val og = other.author
                "${current}$splitter${og}"
            } else other.author

        if (other.artist != null)
            artist = if (currentArtist() != originalArtist()) {
                val current = currentArtist()
                val og = other.artist
                "${current}$splitter${og}"
            } else other.artist

        if (other.description != null)
            description = if (currentDesc() != originalDesc()) {
                val current = currentDesc()
                val og = other.description
                "${current}$splitter${og}"
            } else other.description

        if (other.genre != null)
            genre = if (currentGenres() != originalGenres()) {
                val current = currentGenres()
                val og = other.genre
                "${current}$splitter${og}"
            } else other.genre

        if (other.thumbnail_url != null)
            thumbnail_url = other.thumbnail_url

        status = other.status

        if (!initialized)
            initialized = other.initialized
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val splitter = "▒ ▒∩▒"

        fun create(): SManga {
            return MangaImpl()
        }
    }

}