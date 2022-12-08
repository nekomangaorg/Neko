package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

/**
 * Response after login
 */
@Serializable
data class LoginResponseDto(val access_token: String, val refresh_token: String)

@Serializable
data class ErrorResponse(val result: String, val errors: List<ErrorResult>)

@Serializable
data class ErrorResult(val status: Int, val title: String?, val detail: String?)

/**
 * Check if session token is valid
 */
@Serializable
data class CheckTokenDto(val active: Boolean)

/**
 * Request to  refresh token
 */
@Serializable
data class RefreshTokenDto(val token: String)
