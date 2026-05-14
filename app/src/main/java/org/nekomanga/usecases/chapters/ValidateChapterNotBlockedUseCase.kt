package org.nekomanga.usecases.chapters

import org.nekomanga.constants.Constants

class ValidateChapterNotBlockedUseCase {
    operator fun invoke(
        scanlators: List<String>,
        uploader: String?,
        blockedGroups: Set<String>,
        blockedUploaders: Set<String>,
    ): Boolean {
        if (blockedGroups.isEmpty() && blockedUploaders.isEmpty()) return true

        val isGroupBlocked = scanlators.any { it in blockedGroups }
        if (isGroupBlocked) return false

        if (blockedUploaders.isNotEmpty()) {
            val hasNoGroup = scanlators.any { it == Constants.NO_GROUP }
            if (hasNoGroup && uploader in blockedUploaders) return false
        }

        return true
    }
}
