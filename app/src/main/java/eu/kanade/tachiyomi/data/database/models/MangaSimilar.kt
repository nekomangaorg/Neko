package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

/**
 * Object containing the history statistics of a chapter
 */
interface MangaSimilar : Serializable {

    /**
     * Id of this similar manga object.
     */
    var id: Long?

    /**
     * Mangadex id of manga
     */
    var manga_id: String

    /**
     * JSONArray.toString() of our similar manga object
     */
    var data: String

    companion object {
        fun create(): MangaSimilarImpl {
            return MangaSimilarImpl()
        }
    }
}
