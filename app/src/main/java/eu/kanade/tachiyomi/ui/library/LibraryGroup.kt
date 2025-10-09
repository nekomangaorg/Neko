package eu.kanade.tachiyomi.ui.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector
import org.nekomanga.R
import org.nekomanga.presentation.components.icons.ProgressClockIcon
import org.nekomanga.presentation.components.icons.UngroupIcon

sealed class LibraryGroup(val type: Int, @StringRes val nameRes: Int, val icon: ImageVector) {
    data object ByCategory :
        LibraryGroup(0, R.string.categories, Icons.AutoMirrored.Outlined.Label)

    data object ByTag : LibraryGroup(1, R.string.tag, Icons.Default.Style)

    data object ByStatus : LibraryGroup(3, R.string.status, ProgressClockIcon)

    data object ByTrackStatus : LibraryGroup(4, R.string.tracking_status, Icons.Default.Autorenew)

    data object Ungrouped : LibraryGroup(5, R.string.ungrouped, UngroupIcon)

    data object ByAuthor : LibraryGroup(6, R.string.author, Icons.Default.Person)

    data object ByContent : LibraryGroup(7, R.string.content_rating, Icons.Default.LocalLibrary)

    data object ByLanguage : LibraryGroup(8, R.string.original_language, Icons.Default.Language)

    companion object {
        fun fromInt(type: Int): LibraryGroup {
            return entries.firstOrNull { it.type == type } ?: ByCategory
        }

        val entries: List<LibraryGroup>
            get() =
                listOf(
                    ByCategory,
                    ByTag,
                    ByStatus,
                    ByTrackStatus,
                    Ungrouped,
                    ByAuthor,
                    ByContent,
                    ByLanguage,
                )
    }
}
