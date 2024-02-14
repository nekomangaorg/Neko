package eu.kanade.tachiyomi.data.track.myanimelist

import kotlinx.serialization.Serializable

@Serializable
data class OAuth(
    val refresh_token: String,
    val access_token: String,
    val token_type: String,
    val created_at: Long = System.currentTimeMillis(),
    val expires_in: Long,
) {

    // Assumes expired a minute earlier
    private val adjustedExpiresIn: Long = (expires_in - 60) * 1000

    fun isExpired() = created_at + adjustedExpiresIn < System.currentTimeMillis()
}
