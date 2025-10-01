package eu.kanade.tachiyomi.ui.library

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

object LibraryGroup {

    const val BY_DEFAULT = 0
    const val BY_TAG = 1
    const val BY_STATUS = 3
    const val BY_TRACK_STATUS = 4
    const val BY_AUTHOR = 6
    const val BY_CONTENT = 7
    const val BY_LANGUAGE = 8
    const val UNGROUPED = 5

    fun groupTypeStringRes(type: Int): Int {
        return when (type) {
            BY_STATUS -> R.string.status
            BY_TAG -> R.string.tag
            BY_TRACK_STATUS -> R.string.tracking_status
            BY_AUTHOR -> R.string.author
            UNGROUPED -> R.string.ungrouped
            BY_CONTENT -> R.string.content_rating
            BY_LANGUAGE -> R.string.original_language
            else -> R.string.categories
        }
    }

    fun groupTypeComposeIcon(type: Int): ImageVector {
        return when (type) {
            BY_STATUS -> ProgressClockIcon
            BY_TAG -> Icons.Default.Style
            BY_TRACK_STATUS -> Icons.Default.Autorenew
            BY_AUTHOR -> Icons.Default.Person
            UNGROUPED -> UngroupIcon
            BY_CONTENT -> Icons.Default.LocalLibrary
            BY_LANGUAGE -> Icons.Default.Language
            else -> Icons.AutoMirrored.Outlined.Label
        }
    }

    fun groupTypeDrawableRes(type: Int): Int {
        return when (type) {
            BY_STATUS -> R.drawable.ic_progress_clock_24dp
            BY_TAG -> R.drawable.ic_style_24dp
            BY_TRACK_STATUS -> R.drawable.ic_sync_24dp
            BY_AUTHOR -> R.drawable.ic_author_24dp
            UNGROUPED -> R.drawable.ic_ungroup_24dp
            BY_CONTENT -> R.drawable.ic_local_library_24dp
            BY_LANGUAGE -> R.drawable.ic_language_24dp
            else -> R.drawable.ic_label_outline_24dp
        }
    }
}
