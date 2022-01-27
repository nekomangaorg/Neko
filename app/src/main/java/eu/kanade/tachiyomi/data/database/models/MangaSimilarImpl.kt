package eu.kanade.tachiyomi.data.database.models

class MangaSimilarImpl : MangaSimilar {

    override var id: Long? = null

    override lateinit var manga_id: String

    override lateinit var data: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as MangaSimilar

        if (id != other.id) return false
        if (manga_id != other.manga_id) return false
        return data != other.data
    }

    override fun hashCode(): Int {
        return id.hashCode() + manga_id.hashCode()
    }

    companion object {
        const val DELIMITER = "|*|"
    }
}
