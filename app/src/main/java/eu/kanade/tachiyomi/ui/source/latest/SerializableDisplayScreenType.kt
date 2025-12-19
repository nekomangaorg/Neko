package eu.kanade.tachiyomi.ui.source.latest

import kotlinx.serialization.SerialName
import org.nekomanga.presentation.components.UiText

@kotlinx.serialization.Serializable
sealed interface SerializableDisplayScreenType {

    @kotlinx.serialization.Serializable
    @SerialName("latest_chapters")
    object LatestChapters : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("feed_updates")
    object FeedUpdates : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("recently_added")
    object RecentlyAdded : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("popular_new_titles")
    object PopularNewTitles : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("list")
    data class List(val title: String, val listUUID: String) : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("tag")
    data class Tag(val title: String) : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("author_name")
    data class AuthorByName(val name: String) : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("author_uuid")
    data class AuthorByUuid(val name: String, val authorUUID: String) :
        SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("group_name")
    data class GroupByName(val name: String) : SerializableDisplayScreenType

    @kotlinx.serialization.Serializable
    @SerialName("group_uuid")
    data class GroupByUuid(val name: String, val groupUUID: String) : SerializableDisplayScreenType
}

/**
 * Converts the UI model to the serializable data model. Call this just BEFORE you save/serialize
 * the object.
 */
fun DisplayScreenType.toSerializable(): SerializableDisplayScreenType {
    return when (this) {
        is DisplayScreenType.LatestChapters -> SerializableDisplayScreenType.LatestChapters
        is DisplayScreenType.FeedUpdates -> SerializableDisplayScreenType.FeedUpdates
        is DisplayScreenType.RecentlyAdded -> SerializableDisplayScreenType.RecentlyAdded
        is DisplayScreenType.PopularNewTitles -> SerializableDisplayScreenType.PopularNewTitles
        is DisplayScreenType.List -> {
            SerializableDisplayScreenType.List(title = this.title.str, listUUID = this.listUUID)
        }
        is DisplayScreenType.Tag -> SerializableDisplayScreenType.Tag(this.title.str)
        is DisplayScreenType.AuthorByName -> {
            SerializableDisplayScreenType.AuthorByName(name = this.title.str)
        }
        is DisplayScreenType.AuthorWithUuid -> {
            SerializableDisplayScreenType.AuthorByUuid(
                name = this.title.str,
                authorUUID = this.authorUUID,
            )
        }
        is DisplayScreenType.GroupByName -> {
            SerializableDisplayScreenType.GroupByName(name = this.title.str)
        }
        is DisplayScreenType.GroupByUuid -> {
            SerializableDisplayScreenType.GroupByUuid(
                name = this.title.str,
                groupUUID = this.groupUUID,
            )
        }
    }
}

/**
 * Converts the serializable data model back to the UI model. Call this just AFTER you
 * load/deserialize the object.
 */
fun SerializableDisplayScreenType.toDomain(): DisplayScreenType {
    return when (this) {
        is SerializableDisplayScreenType.LatestChapters -> DisplayScreenType.LatestChapters
        is SerializableDisplayScreenType.FeedUpdates -> DisplayScreenType.FeedUpdates
        is SerializableDisplayScreenType.RecentlyAdded -> DisplayScreenType.RecentlyAdded
        is SerializableDisplayScreenType.PopularNewTitles -> DisplayScreenType.PopularNewTitles
        is SerializableDisplayScreenType.List -> {
            DisplayScreenType.List(title = UiText.String(this.title), listUUID = this.listUUID)
        }
        is SerializableDisplayScreenType.Tag -> DisplayScreenType.Tag(UiText.String(this.title))
        is SerializableDisplayScreenType.AuthorByName -> {
            DisplayScreenType.AuthorByName(title = UiText.String(this.name))
        }
        is SerializableDisplayScreenType.AuthorByUuid -> {
            DisplayScreenType.AuthorWithUuid(
                title = UiText.String(this.name),
                authorUUID = this.authorUUID,
            )
        }
        is SerializableDisplayScreenType.GroupByName -> {
            DisplayScreenType.GroupByName(title = UiText.String(this.name))
        }
        is SerializableDisplayScreenType.GroupByUuid -> {
            DisplayScreenType.GroupByUuid(
                title = UiText.String(this.name),
                groupUUID = this.groupUUID,
            )
        }
    }
}
