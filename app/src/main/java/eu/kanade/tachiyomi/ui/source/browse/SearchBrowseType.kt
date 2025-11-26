package eu.kanade.tachiyomi.ui.source.browse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface BrowseOption

@Serializable
sealed class QueryType : BrowseOption {
    abstract val query: String

    @Serializable
    @SerialName("title")
    data class Title(override val query: String) : QueryType()

    @Serializable
    @SerialName("tag")
    data class Tag(override val query: String) : QueryType()

    @Serializable
    @SerialName("creator_text")
    data class Creator(override val query: String) : QueryType()
}

@Serializable
sealed class UuidType : BrowseOption {
    abstract val uuid: String

    @Serializable
    @SerialName("manga")
    data class Manga(override val uuid: String) : UuidType()

    @Serializable
    @SerialName("list")
    data class List(override val uuid: String) : UuidType()

    @Serializable
    @SerialName("group")
    data class Group(override val uuid: String) : UuidType()

    @Serializable
    @SerialName("creator_uuid")
    data class Creator(override val uuid: String) : UuidType()
}

@Serializable
data class SearchBrowse(val type: BrowseOption)
