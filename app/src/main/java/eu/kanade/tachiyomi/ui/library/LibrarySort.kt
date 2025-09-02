package eu.kanade.tachiyomi.ui.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.ui.base.MaterialMenuSheet
import org.nekomanga.R

enum class LibrarySort(
    val mainValue: Int,
    @param:StringRes private val stringRes: Int,
    @param:DrawableRes private val iconRes: Int,
    private val catValue: Int = mainValue,
    @param:StringRes private val dynamicStringRes: Int = stringRes,
    @param:DrawableRes private val dynamicIconRes: Int = iconRes,
) {

    Title(0, R.string.title, R.drawable.ic_sort_by_alpha_24dp),
    LastRead(1, R.string.last_read, R.drawable.ic_recent_read_outline_24dp, 3),
    LatestChapter(2, R.string.latest_chapter, R.drawable.ic_new_releases_24dp, 1),
    Unread(3, R.string.unread, R.drawable.ic_eye_24dp, 2),
    TotalChapters(4, R.string.total_chapters, R.drawable.ic_sort_by_numeric_24dp),
    DateAdded(5, R.string.date_added, R.drawable.ic_heart_outline_24dp),
    DateFetched(6, R.string.date_fetched, R.drawable.ic_calendar_text_outline_24dp),
    DragAndDrop(
        7,
        R.string.drag_and_drop,
        R.drawable.ic_swap_vert_24dp,
        7,
        R.string.category,
        R.drawable.ic_label_outline_24dp,
    ),
    Rating(8, R.string.rating, R.drawable.ic_poll_24dp);

    val categoryValue: Char
        get() = if (this == DragAndDrop) 'D' else 'a' + catValue * 2

    val categoryValueDescending: Char
        get() = if (this == DragAndDrop) 'D' else 'b' + catValue * 2

    @StringRes
    fun stringRes(isDynamic: Boolean = false) = if (isDynamic) dynamicStringRes else stringRes

    @DrawableRes
    fun iconRes(isDynamic: Boolean = false) = if (isDynamic) dynamicIconRes else iconRes

    fun composeIcon(): ImageVector {
        return when (this) {
            Title -> Icons.Default.SortByAlpha
            LastRead -> Icons.Outlined.AccessTime
            LatestChapter -> Icons.Outlined.NewReleases
            Unread -> Icons.Filled.RemoveRedEye
            TotalChapters -> Icons.Filled.FormatListNumbered
            DateAdded -> Icons.Outlined.Favorite
            DateFetched -> Icons.Outlined.CalendarMonth
            DragAndDrop -> Icons.Outlined.SwapVert
            Rating -> Icons.Default.BarChart
        }
    }

    fun menuSheetItem(isDynamic: Boolean): MaterialMenuSheet.MenuSheetItem {
        return MaterialMenuSheet.MenuSheetItem(mainValue, iconRes(isDynamic), stringRes(isDynamic))
    }

    companion object {
        fun filteredEntries() = entries.filterNot { it == DragAndDrop }

        fun filteredValueOf(char: Char?) =
            filteredEntries().find {
                it.categoryValue == char || it.categoryValueDescending == char
            }

        fun valueOf(value: Int) = entries.find { it.mainValue == value }

        fun valueOf(char: Char?) =
            entries.find { it.categoryValue == char || it.categoryValueDescending == char }
    }
}
