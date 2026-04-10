package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MangaBakaUserRole {
    @SerialName("user") USER,
    @SerialName("developer") DEVELOPER,
    @SerialName("contributor") CONTRIBUTOR,
    @SerialName("moderator") MODERATOR,
    @SerialName("admin") ADMIN
}

@Serializable
enum class MangaBakaAuthType {
    @SerialName("oauth") OAUTH,
    @SerialName("pat") PAT,
    @SerialName("session") SESSION
}

@Serializable
enum class MangaBakaAuthScope {
    @SerialName("library.read") LIBRARY_READ,
    @SerialName("library.write") LIBRARY_WRITE,
    @SerialName("mod") MOD,
    @SerialName("openid") OPENID,
    @SerialName("profile") PROFILE,
    @SerialName("offline_access") OFFLINE_ACCESS
}

@Serializable
enum class MangaBakaLibraryState {
    @SerialName("considering") CONSIDERING,
    @SerialName("completed") COMPLETED,
    @SerialName("dropped") DROPPED,
    @SerialName("paused") PAUSED,
    @SerialName("plan_to_read") PLAN_TO_READ,
    @SerialName("reading") READING,
    @SerialName("rereading") REREADING
}
