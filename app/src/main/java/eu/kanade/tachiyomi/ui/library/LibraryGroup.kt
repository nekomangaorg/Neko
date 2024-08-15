package eu.kanade.tachiyomi.ui.library

import org.nekomanga.R

object LibraryGroup {

    const val BY_DEFAULT = 0
    const val BY_TAG = 1
    const val BY_STATUS = 3
    const val BY_TRACK_STATUS = 4
    const val BY_AUTHOR = 6
    const val BY_CONTENT = 7
    const val BY_LANGUAGE = 8
    const val BY_LIST = 9
    const val UNGROUPED = 5

    fun groupTypeStringRes(type: Int, hasCategories: Boolean = true): Int {
        return when (type) {
            BY_STATUS -> R.string.status
            BY_TAG -> R.string.tag
            BY_TRACK_STATUS -> R.string.tracking_status
            BY_AUTHOR -> R.string.author
            UNGROUPED -> R.string.ungrouped
            BY_CONTENT -> R.string.content_rating
            BY_LANGUAGE -> R.string.original_language
            BY_LIST -> R.string.mdlists_
            else -> if (hasCategories) R.string.categories else R.string.ungrouped
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
            BY_LIST -> R.drawable.ic_view_list_24dp
            else -> R.drawable.ic_label_outline_24dp
        }
    }
}
