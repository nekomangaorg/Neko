package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaLibraryListEntry(
    @SerialName("library_entry_id")
    val libraryEntryId: Int?,

    @SerialName("library_list_id")
    val libraryListId: Int?
)

@Serializable
data class MangaBakaLibraryEntry(
    val id: Int? = null,

    @SerialName("user_id")
    val userId: String,

    @SerialName("series_id")
    val seriesId: Int? = null,

    val note: String? = null,

    @SerialName("read_link")
    val readLink: String? = null,

    val rating: Double? = null,

    val state: MangaBakaLibraryState,

    val priority: Double? = 20.0,

    @SerialName("is_private")
    val isPrivate: Boolean? = false,

    @SerialName("number_of_rereads")
    val numberOfRereads: Int? = null,

    @SerialName("progress_chapter")
    val progressChapter: Double? = null,

    @SerialName("progress_volume")
    val progressVolume: Double? = null,

    @SerialName("start_date")
    val startDate: String? = null,

    @SerialName("finish_date")
    val finishDate: String? = null,

    // Note: The schema specifies these keys with Capital letters
    @SerialName("Entries")
    val entries: List<MangaBakaLibraryListEntry>? = emptyList(),

    @SerialName("Series")
    val series: MangaBakaV1Series? = null
)
