package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.track.TrackService

object MangaConstants {

    sealed class MergedManga {
        class IsMerged(val url: String) : MergedManga()
        object NotMerged : MergedManga()
    }

    class CategoryActions(
        val set: (List<Category>) -> Unit = {},
        val addNew: (String) -> Unit = {},
    )

    class TrackActions(
        val statusChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val scoreChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val chapterChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val dateChange: (TrackingConstants.TrackDateChange) -> Unit,
        val search: (String, TrackService) -> Unit,
        val searchItemClick: (TrackingConstants.TrackAndService) -> Unit,
        val remove: (Boolean, TrackService) -> Unit,
    )

    class CoverActions(
        val share: (Context, String) -> Unit,
        val set: (String) -> Unit,
        val save: (String) -> Unit,
        val reset: () -> Unit,
    )

    class MergeActions(
        val remove: () -> Unit,
    )
}
