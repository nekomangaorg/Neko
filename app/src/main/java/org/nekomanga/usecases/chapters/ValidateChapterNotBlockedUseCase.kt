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

        val hasBlockedGroup = scanlators.any { it in blockedGroups }
        val hasBlockedUploader = Constants.NO_GROUP in scanlators && uploader in blockedUploaders

        return !hasBlockedGroup && !hasBlockedUploader
    }
}
