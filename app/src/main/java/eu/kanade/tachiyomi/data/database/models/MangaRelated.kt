package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

/**
 * Object containing the history statistics of a chapter
 */
interface MangaRelated : Serializable {

    /**
     * Id of this related manga object.
     */
    var id: Long?

    /**
     * Id of matching manga
     */
    var manga_id: Long?

    /**
     * Comma separated string of other manga ids
     * Example: 3467, 5907, 21052, 2141, 6139, 5602, 3999
     */
    var matched_ids: String

    /**
     * Comma separated string of other manga titles
     * Example: Title1, Title2, ..., Title10
     */
    var matched_titles: String

    /**
     * Comma separated string of other manga scores
     * Example: 0.027, 0.027, 0.027, 0.026, 0.024, 0.023
     */
    var scores: String


}
