package eu.kanade.tachiyomi.source.model

class SMangaImpl : SManga {

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var lang_flag: String? = null

    override var status: Int = 0

    override var follow_status: SManga.FollowStatus? = null

    override var thumbnail_url: String? = null

    override var initialized: Boolean = false

}