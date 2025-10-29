package eu.kanade.tachiyomi.ui.source.latest

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.nekomanga.R

sealed interface DisplayScreenType : Parcelable {
    @Parcelize
    data class LatestChapters(@StringRes val titleRes: Int = R.string.latest) : DisplayScreenType

    @Parcelize
    data class FeedUpdates(@StringRes val titleRes: Int = R.string.feed_updates) :
        DisplayScreenType

    @Parcelize
    data class RecentlyAdded(@StringRes val titleRes: Int = R.string.recently_added) :
        DisplayScreenType

    @Parcelize
    data class PopularNewTitles(@StringRes val titleRes: Int = R.string.popular_new_titles) :
        DisplayScreenType

    @Parcelize
    data class List(val title: String, val listUUID: String) : DisplayScreenType

    @Parcelize
    data class Similar(val mangaId: String) : DisplayScreenType
}
