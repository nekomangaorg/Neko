package org.nekomanga.network.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class MangaBakaUserProfileResponse(val data: MangaBakaProfile)

@Serializable
data class MangaBakaProfile(
    val id: String,
    val nickname: String? = null,
    @SerialName("preferred_username") val preferredUsername: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val role: MangaBakaUserRole,
    @SerialName("auth_type") val authType: MangaBakaAuthType,
    val scopes: List<MangaBakaAuthScope>,
    @SerialName("rating_steps") val ratingSteps: Int = 1,
    @SerialName("library_default_state")
    val libraryDefaultState: MangaBakaLibraryState? = MangaBakaLibraryState.PLAN_TO_READ,
    @SerialName("accepted_contribution_terms") val acceptedContributionTerms: Boolean? = false,
)
