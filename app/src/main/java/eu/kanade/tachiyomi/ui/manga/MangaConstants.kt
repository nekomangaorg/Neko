package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.track.TrackService

object MangaConstants {
    class CategoryActions(
        val setCategories: (List<Category>) -> Unit = {},
        val addNewCategory: (String) -> Unit = {},
    )

    class TrackActions(
        val trackStatusChanged: (Int, TrackingConstants.TrackAndService) -> Unit,
        val trackScoreChanged: (Int, TrackingConstants.TrackAndService) -> Unit,
        val trackChapterChanged: (Int, TrackingConstants.TrackAndService) -> Unit,
        val searchTracker: (String, TrackService) -> Unit,
        val trackSearchItemClick: (TrackingConstants.TrackAndService) -> Unit,
        val trackingRemoved: (Boolean, TrackService) -> Unit,
        val trackingDateChanged: (TrackingConstants.TrackDateChange) -> Unit,
    )

    class CoverActions(
        val share: (Context, String) -> Unit,
        val set: (String) -> Unit,
        val save: (String) -> Unit,
    )
}
