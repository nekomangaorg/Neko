package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SManga

open class MangaImpl : Manga {

    override var id: Long? = null

    override var source: Long = -1

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var status: Int = 0

    override var thumbnail_url: String? = null

    override var favorite: Boolean = false

    override var last_update: Long = 0

    override var initialized: Boolean = false

    override var viewer: Int = 0

    override var chapter_flags: Int = 0

    override var hide_title: Boolean = false

    var last_cover_fetch: Long = 0

    override fun copyFrom(other: SManga) {
        if (((other is MangaImpl && (other as MangaImpl)::title.isInitialized)
                || other !is MangaImpl) && other.title != title) {
            title = if (customTitle() != trueTitle()) {
                val customTitle = customTitle()
                val trueTitle = other.title
                "${customTitle}≡§${trueTitle}"
            } else other.title
        }
        super.copyFrom(other)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val manga = other as Manga

        return url == manga.url

    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

}
