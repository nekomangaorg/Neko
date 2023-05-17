package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val data: UserData,
)

@Serializable
data class UserData(
    val id: String,
    val attributes: UserAttributes,
)

@Serializable
data class UserAttributes(
    val username: String,
)
