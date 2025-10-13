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
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.presentation.components.icons.ProgressClockIcon
import org.nekomanga.presentation.components.icons.UngroupIcon

sealed class LibraryGroup(
    val type: Int,
    @StringRes val nameRes: Int,
    val icon: ImageVector,
    val keyComparator: Comparator<String>,
) {
    data object ByCategory :
        LibraryGroup(
            type = 0,
            nameRes = R.string.categories,
            icon = Icons.AutoMirrored.Outlined.Label,
            keyComparator = String.CASE_INSENSITIVE_ORDER,
        )

    data object ByTag :
        LibraryGroup(
            type = 1,
            nameRes = R.string.tag,
            icon = Icons.Default.Style,
            keyComparator = String.CASE_INSENSITIVE_ORDER,
        )

    data object ByStatus :
        LibraryGroup(
            type = 3,
            nameRes = R.string.status,
            icon = ProgressClockIcon,
            keyComparator = String.CASE_INSENSITIVE_ORDER,
        )

    data object ByTrackStatus :
        LibraryGroup(
            type = 4,
            nameRes = R.string.tracking_status,
            icon = Icons.Default.Autorenew,
            keyComparator =
                compareBy {
                    when (it.lowercase()) {
                        "re reading" -> 1
                        "plan to read" -> 2
                        "on hold" -> 3
                        "completed" -> 4
                        "dropped" -> 5
                        "unfollowed" -> 6
                        else -> 0
                    }
                },
        )

    data object Ungrouped :
        LibraryGroup(
            type = 5,
            nameRes = R.string.ungrouped,
            icon = UngroupIcon,
            keyComparator = String.CASE_INSENSITIVE_ORDER,
        )

    data object ByAuthor :
        LibraryGroup(
            type = 6,
            nameRes = R.string.author,
            icon = Icons.Default.Person,
            keyComparator = String.CASE_INSENSITIVE_ORDER,
        )

    data object ByContent :
        LibraryGroup(
            type = 7,
            nameRes = R.string.content_rating,
            icon = Icons.Default.LocalLibrary,
            keyComparator =
                compareBy {
                    when (it.lowercase()) {
                        MangaContentRating.Suggestive.key -> 1
                        MangaContentRating.Erotica.key -> 2
                        MangaContentRating.Pornographic.key -> 3
                        MangaContentRating.Unknown.key -> 4
                        else -> 0 // Other statuses
                    }
                },
        )

    data object ByLanguage :
        LibraryGroup(
            type = 8,
            nameRes = R.string.original_language,
            icon = Icons.Default.Language,
            keyComparator = String.CASE_INSENSITIVE_ORDER,
        )

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
