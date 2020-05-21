package eu.kanade.tachiyomi.ui.reader.chapter

import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.setBottomEdge
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import kotlinx.android.synthetic.main.chapter_filter_layout.*
import kotlinx.android.synthetic.main.chapter_filter_sheet.*
import kotlinx.android.synthetic.main.reader_chapters_sheet.*

class ChapterFilterSheet(activity: ReaderActivity, manga: Manga) :
    BottomSheetDialog(activity, R.style.BottomSheetDialogTheme) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.chapter_filter_sheet, null)
        setContentView(view)
        BottomSheetBehavior.from(view.parent as ViewGroup).expand()
        setEdgeToEdge(activity, view)
        setBottomEdge(show_bookmark, activity)

        chapter_filter_layout.setCheckboxes(manga)
        setOnDismissListener {
            activity.presenter.setFilters(
                show_read.isChecked,
                show_unread.isChecked,
                show_download.isChecked,
                show_bookmark.isChecked
            )
            activity.chapters_bottom_sheet.refreshList()
        }
    }
}
