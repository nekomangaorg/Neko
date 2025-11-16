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
            // We extract the raw string from UiText.String
            SerializableDisplayScreenType.List(title = this.title.str, listUUID = this.listUUID)
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
            // We reconstruct the UiText.String from the raw string
            DisplayScreenType.List(title = UiText.String(this.title), listUUID = this.listUUID)
        }
    }
}
