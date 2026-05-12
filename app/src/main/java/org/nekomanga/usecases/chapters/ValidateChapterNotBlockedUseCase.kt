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

        var hasNoGroup = false
        for (scanlator in scanlators) {
            if (scanlator in blockedGroups) return false
            if (scanlator == Constants.NO_GROUP) hasNoGroup = true
        }

        return !(hasNoGroup && uploader in blockedUploaders)
    }
}
