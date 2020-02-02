package eu.kanade.tachiyomi.data.database.models



class MangaRelatedImpl : MangaRelated {

    override var id: Long? = null

    override var manga_id: Long? = null

    override lateinit var matched_ids: String

    override lateinit var matched_titles: String

    override lateinit var scores: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as MangaRelated

        if (id != other.id) return false
        if (manga_id != other.manga_id) return false
        if (matched_ids != other.matched_ids) return false
        if (matched_titles != other.matched_titles) return false
        return scores == other.scores
    }

    override fun hashCode(): Int {
        return id.hashCode()+manga_id.hashCode()
    }

}
